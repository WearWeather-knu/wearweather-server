package wearweather.wearweather_server.application.clothes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@Component
public class RemoteImageClient {
    private static final int MAX_REDIRECTS = 3;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final int maxImageBytes;

    public RemoteImageClient(
            @Value("${app.clothes-import.connect-timeout-seconds:3}") long connectTimeoutSeconds,
            @Value("${app.clothes-import.request-timeout-seconds:8}") long requestTimeoutSeconds,
            @Value("${app.clothes-import.max-image-bytes:5242880}") int maxImageBytes
    ) {
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
        this.maxImageBytes = maxImageBytes;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public RemoteImage download(String imageUrl) {
        try {
            return download(URI.create(imageUrl), 0);
        } catch (IllegalArgumentException exception) {
            throw new ClothesImportException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PRODUCT_IMAGE",
                    "상품 이미지 URL이 올바르지 않습니다.", exception);
        }
    }

    private RemoteImage download(URI uri, int redirects) {
        validateImageUri(uri);
        if (redirects > MAX_REDIRECTS) {
            throw new ClothesImportException(HttpStatus.BAD_GATEWAY, "IMAGE_REDIRECT_FAILED",
                    "상품 이미지 리다이렉트가 너무 많습니다.");
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "image/jpeg,image/png,image/webp")
                .header("User-Agent", "Mozilla/5.0 WearWeatherProductImporter/1.0")
                .GET()
                .build();
        try {
            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                String location = response.headers().firstValue("location")
                        .orElseThrow(() -> new ClothesImportException(HttpStatus.BAD_GATEWAY,
                                "IMAGE_REDIRECT_FAILED", "이미지 리다이렉트 위치가 없습니다."));
                response.body().close();
                return download(uri.resolve(location), redirects + 1);
            }
            if (response.statusCode() != 200) {
                response.body().close();
                throw new ClothesImportException(HttpStatus.BAD_GATEWAY, "IMAGE_FETCH_FAILED",
                        "상품 이미지를 불러오지 못했습니다.");
            }
            String contentType = response.headers().firstValue("content-type")
                    .map(value -> value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT))
                    .orElse("");
            String extension = EXTENSIONS.get(contentType);
            if (extension == null) {
                response.body().close();
                throw new ClothesImportException(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_IMAGE_TYPE",
                        "JPEG, PNG, WEBP 상품 이미지만 지원합니다.");
            }
            byte[] bytes = LimitedBodyReader.read(response.body(), maxImageBytes);
            if (!hasValidSignature(bytes, contentType)) {
                throw new ClothesImportException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_IMAGE_CONTENT",
                        "상품 이미지의 실제 형식이 응답 MIME 타입과 일치하지 않습니다.");
            }
            return new RemoteImage(bytes, contentType, extension);
        } catch (LimitedBodyReader.BodyTooLargeException exception) {
            throw new ClothesImportException(HttpStatus.PAYLOAD_TOO_LARGE, "PRODUCT_IMAGE_TOO_LARGE",
                    "상품 이미지 크기가 허용 범위를 초과했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ClothesImportException(HttpStatus.BAD_GATEWAY, "IMAGE_FETCH_INTERRUPTED",
                    "상품 이미지 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new ClothesImportException(HttpStatus.BAD_GATEWAY, "IMAGE_FETCH_FAILED",
                    "상품 이미지를 불러오지 못했습니다.", exception);
        }
    }

    private void validateImageUri(URI uri) {
        String host = uri.getHost();
        String normalized = host == null ? "" : host.toLowerCase(Locale.ROOT);
        boolean allowed = normalized.equals("msscdn.net") || normalized.endsWith(".msscdn.net")
                || normalized.equals("musinsa.com") || normalized.endsWith(".musinsa.com");
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !allowed || uri.getUserInfo() != null || uri.getPort() != -1) {
            throw new ClothesImportException(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_IMAGE_HOST",
                    "허용되지 않은 상품 이미지 주소입니다.");
        }
    }

    private boolean hasValidSignature(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                    && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }
}
