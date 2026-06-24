package wearweather.wearweather_server.infrastructure.clothes;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import wearweather.wearweather_server.application.clothes.ClothesImportException;
import wearweather.wearweather_server.application.clothes.MusinsaProduct;
import wearweather.wearweather_server.application.clothes.port.ProductImportPort;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;
import wearweather.wearweather_server.infrastructure.common.LimitedBodyReader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MusinsaProductClient implements ProductImportPort {
    private static final Pattern PRODUCT_PATH = Pattern.compile("^/(?:products|app/goods)/(\\d+)(?:/.*)?$");
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_DESCRIPTION_CHARS = 12_000;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration requestTimeout;
    private final int maxHtmlBytes;

    public MusinsaProductClient(
            ObjectMapper objectMapper,
            @Value("${app.clothes-import.connect-timeout-seconds:3}") long connectTimeoutSeconds,
            @Value("${app.clothes-import.request-timeout-seconds:8}") long requestTimeoutSeconds,
            @Value("${app.clothes-import.max-html-bytes:2097152}") int maxHtmlBytes
    ) {
        this.objectMapper = objectMapper;
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
        this.maxHtmlBytes = maxHtmlBytes;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public MusinsaProduct fetch(String originalUrl) {
        CanonicalProduct canonical = canonicalize(originalUrl);
        byte[] html = fetchHtml(URI.create(canonical.url()), 0);
        return parse(canonical, new String(html, StandardCharsets.UTF_8));
    }

    public String canonicalUrl(String originalUrl) {
        return canonicalize(originalUrl).url();
    }

    MusinsaProduct parseHtml(String originalUrl, String html) {
        CanonicalProduct canonical = canonicalize(originalUrl);
        return parse(canonical, html);
    }

    private CanonicalProduct canonicalize(String originalUrl) {
        try {
            URI uri = URI.create(originalUrl == null ? "" : originalUrl.trim());
            validateMusinsaUri(uri);
            Matcher matcher = PRODUCT_PATH.matcher(uri.getPath());
            if (!matcher.matches()) {
                throw invalidUrl("무신사 상품 URL 형식이 아닙니다.");
            }
            String productId = matcher.group(1);
            return new CanonicalProduct(productId, "https://www.musinsa.com/products/" + productId);
        } catch (IllegalArgumentException exception) {
            throw invalidUrl("유효한 무신사 상품 URL을 입력해주세요.");
        }
    }

    private byte[] fetchHtml(URI uri, int redirects) {
        validateMusinsaUri(uri);
        if (redirects > MAX_REDIRECTS) {
            throw new ClothesImportException(HttpStatus.BAD_GATEWAY, "TOO_MANY_REDIRECTS",
                    "무신사 페이지 리다이렉트가 너무 많습니다.");
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("User-Agent", "Mozilla/5.0 WearWeatherProductImporter/1.0")
                .GET()
                .build();
        try {
            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                String location = response.headers().firstValue("location")
                        .orElseThrow(() -> new ClothesImportException(HttpStatus.BAD_GATEWAY,
                                "INVALID_REDIRECT", "무신사 응답의 리다이렉트 위치가 없습니다."));
                response.body().close();
                return fetchHtml(uri.resolve(location), redirects + 1);
            }
            if (response.statusCode() != 200) {
                response.body().close();
                throw new ClothesImportException(HttpStatus.BAD_GATEWAY, "PRODUCT_FETCH_FAILED",
                        "무신사 상품 페이지를 불러오지 못했습니다.");
            }
            String contentType = response.headers().firstValue("content-type").orElse("");
            if (!contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
                response.body().close();
                throw new ClothesImportException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PRODUCT_PAGE",
                        "상품 페이지가 HTML 형식이 아닙니다.");
            }
            return LimitedBodyReader.read(response.body(), maxHtmlBytes);
        } catch (LimitedBodyReader.BodyTooLargeException exception) {
            throw new ClothesImportException(HttpStatus.PAYLOAD_TOO_LARGE, "PRODUCT_PAGE_TOO_LARGE",
                    "상품 페이지 크기가 허용 범위를 초과했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ClothesImportException(HttpStatus.BAD_GATEWAY, "PRODUCT_FETCH_INTERRUPTED",
                    "상품 페이지 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new ClothesImportException(HttpStatus.BAD_GATEWAY, "PRODUCT_FETCH_FAILED",
                    "무신사 상품 페이지를 불러오지 못했습니다.", exception);
        }
    }

    private MusinsaProduct parse(CanonicalProduct canonical, String html) {
        Document document = Jsoup.parse(html, canonical.url());
        ProductMetadata metadata = new ProductMetadata();

        for (Element script : document.select("script[type=application/ld+json]")) {
            readJsonMetadata(script.data(), metadata);
        }
        for (Element script : document.select("script[type=application/json]")) {
            readCategoryMetadata(script.data(), metadata);
        }

        if (blank(metadata.name)) {
            metadata.name = meta(document, "meta[property=og:title]");
        }
        if (blank(metadata.imageUrl)) {
            metadata.imageUrl = meta(document, "meta[property=og:image]");
        }
        if (blank(metadata.description)) {
            metadata.description = meta(document, "meta[property=og:description]");
        }
        if (blank(metadata.description)) {
            metadata.description = meta(document, "meta[name=description]");
        }

        metadata.name = cleanName(metadata.name);
        if (blank(metadata.name) || blank(metadata.imageUrl)) {
            throw new ClothesImportException(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCT_PARSE_FAILED",
                    "상품명 또는 대표 이미지를 찾지 못했습니다.");
        }

        String categoryText = String.join(" ", metadata.categoryValues) + " " + metadata.name + " " + metadata.description;
        ClothesCategory detectedCategory = detectCategory(categoryText);
        return new MusinsaProduct(
                canonical.productId(),
                canonical.url(),
                metadata.name,
                metadata.imageUrl,
                truncate(metadata.description, MAX_DESCRIPTION_CHARS),
                detectedCategory
        );
    }

    private void readJsonMetadata(String json, ProductMetadata metadata) {
        if (blank(json)) return;
        try {
            visitProductNode(objectMapper.readTree(json), metadata);
        } catch (RuntimeException ignored) {
            // A broken third-party JSON-LD block must not abort the full parser chain.
        }
    }

    private void visitProductNode(JsonNode node, ProductMetadata metadata) {
        if (node == null) return;
        JsonNode type = node.get("@type");
        if (type != null && "product".equalsIgnoreCase(type.asText())) {
            if (blank(metadata.name)) metadata.name = text(node.get("name"));
            if (blank(metadata.description)) metadata.description = text(node.get("description"));
            if (blank(metadata.imageUrl)) metadata.imageUrl = firstText(node.get("image"));
        }
        node.forEach(child -> visitProductNode(child, metadata));
    }

    private void readCategoryMetadata(String json, ProductMetadata metadata) {
        if (blank(json)) return;
        try {
            visitCategoryNode(objectMapper.readTree(json), metadata);
        } catch (RuntimeException ignored) {
            // The Open Graph and JSON-LD fallbacks remain available.
        }
    }

    private void visitCategoryNode(JsonNode node, ProductMetadata metadata) {
        if (node == null) return;
        for (String key : List.of("categoryName", "category_name", "goodsCategoryName", "category")) {
            JsonNode value = node.get(key);
            if (value != null && value.isTextual() && metadata.categoryValues.size() < 20) {
                metadata.categoryValues.add(value.asText());
            }
        }
        node.forEach(child -> visitCategoryNode(child, metadata));
    }

    private ClothesCategory detectCategory(String value) {
        String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        if (containsAny(text, "백팩", "토트", "숄더백", "크로스백", "클러치", "더플백", "가방")) return ClothesCategory.BAG;
        if (containsAny(text, "스니커즈", "운동화", "부츠", "로퍼", "구두", "샌들", "슬리퍼", "신발")) return ClothesCategory.SHOES;
        if (containsAny(text, "코트", "재킷", "자켓", "패딩", "점퍼", "가디건", "아우터")) return ClothesCategory.OUTER;
        if (containsAny(text, "팬츠", "바지", "스커트", "슬랙스", "쇼츠", "하의")) return ClothesCategory.BOTTOM;
        if (containsAny(text, "티셔츠", "셔츠", "니트", "스웨트", "후드", "민소매", "상의")) return ClothesCategory.TOP;
        if (containsAny(text, "모자", "캡", "비니", "스카프", "장갑", "벨트", "액세서리")) return ClothesCategory.ACC;
        return null;
    }

    private void validateMusinsaUri(URI uri) {
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                || !(host.equalsIgnoreCase("musinsa.com") || host.toLowerCase(Locale.ROOT).endsWith(".musinsa.com"))) {
            throw invalidUrl("HTTPS 무신사 상품 URL만 지원합니다.");
        }
        if (uri.getUserInfo() != null || uri.getPort() != -1) {
            throw invalidUrl("사용자 정보나 포트가 포함된 URL은 지원하지 않습니다.");
        }
    }

    private ClothesImportException invalidUrl(String message) {
        return new ClothesImportException(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_PRODUCT_URL", message);
    }

    private String meta(Document document, String selector) {
        Element element = document.selectFirst(selector);
        return element == null ? null : element.attr("content").trim();
    }

    private String text(JsonNode node) {
        return node != null && node.isTextual() ? node.asText().trim() : null;
    }

    private String firstText(JsonNode node) {
        if (node == null) return null;
        if (node.isTextual()) return node.asText().trim();
        if (node.isArray() && !node.isEmpty()) return firstText(node.get(0));
        return null;
    }

    private String cleanName(String name) {
        if (name == null) return null;
        return name.replaceFirst("\\s*-\\s*(사이즈\\s*&\\s*)?후기\\s*\\|\\s*무신사.*$", "").trim();
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) if (text.contains(candidate)) return true;
        return false;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record CanonicalProduct(String productId, String url) {
    }

    private static final class ProductMetadata {
        private String name;
        private String imageUrl;
        private String description;
        private final List<String> categoryValues = new ArrayList<>();
    }
}
