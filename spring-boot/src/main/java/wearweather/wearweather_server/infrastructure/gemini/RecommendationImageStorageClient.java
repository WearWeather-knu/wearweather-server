package wearweather.wearweather_server.infrastructure.gemini;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import wearweather.wearweather_server.application.gemini.RecommendationException;
import wearweather.wearweather_server.application.gemini.port.RecommendationImageStoragePort;
import wearweather.wearweather_server.application.gemini.port.RecommendationImageStoragePort.StoredImage;
import wearweather.wearweather_server.application.gemini.port.RecommendationImageStoragePort.StoredObject;
import wearweather.wearweather_server.infrastructure.common.LimitedBodyReader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class RecommendationImageStorageClient implements RecommendationImageStoragePort {
    private static final Logger log = LoggerFactory.getLogger(RecommendationImageStorageClient.class);
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final RestClient restClient;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucket;
    private final int maxImageBytes;

    public RecommendationImageStorageClient(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.service-role-key:}") String serviceRoleKey,
            @Value("${supabase.storage.clothes-bucket:clothes-images}") String bucket,
            @Value("${app.outfit-recommendation.connect-timeout-seconds:5}") long connectTimeoutSeconds,
            @Value("${app.outfit-recommendation.request-timeout-seconds:30}") long requestTimeoutSeconds,
            @Value("${app.outfit-recommendation.max-image-bytes:5242880}") int maxImageBytes
    ) {
        this.supabaseUrl = removeTrailingSlash(supabaseUrl);
        this.serviceRoleKey = serviceRoleKey;
        this.bucket = bucket;
        this.maxImageBytes = maxImageBytes;
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(requestTimeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public StoredImage download(String imageUrl) {
        validateConfiguration();
        URI uri;
        try {
            uri = URI.create(imageUrl);
        } catch (RuntimeException exception) {
            throw invalidStoredImage("저장된 옷 이미지 URL이 올바르지 않습니다.", exception);
        }
        URI storageOrigin = URI.create(supabaseUrl);
        String allowedPathPrefix = "/storage/v1/object/public/" + bucket + "/";
        URI normalized = uri.normalize();
        boolean allowedOrigin = storageOrigin.getScheme() != null && storageOrigin.getHost() != null
                && uri.getScheme() != null && uri.getHost() != null
                && storageOrigin.getScheme().equalsIgnoreCase(uri.getScheme())
                && storageOrigin.getHost().equalsIgnoreCase(uri.getHost())
                && storageOrigin.getPort() == uri.getPort();
        boolean allowedPath = normalized.equals(uri) && uri.getPath() != null
                && uri.getPath().startsWith(allowedPathPrefix)
                && uri.getPath().length() > allowedPathPrefix.length();
        if (!allowedOrigin || !allowedPath || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw invalidStoredImage("허용되지 않은 옷 이미지 URL입니다.", null);
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "image/jpeg,image/png,image/webp")
                .GET()
                .build();
        try {
            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                throw new RecommendationException(HttpStatus.BAD_GATEWAY, "CLOTHES_IMAGE_FETCH_FAILED",
                        "저장된 옷 이미지를 불러오지 못했습니다.");
            }
            String contentType = response.headers().firstValue("content-type")
                    .map(value -> value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT))
                    .orElse("");
            if (!EXTENSIONS.containsKey(contentType)) {
                response.body().close();
                throw invalidStoredImage("지원하지 않는 옷 이미지 형식입니다.", null);
            }
            byte[] bytes = LimitedBodyReader.read(response.body(), maxImageBytes);
            if (!hasValidSignature(bytes, contentType)) {
                throw invalidStoredImage("옷 이미지의 실제 형식이 MIME 타입과 일치하지 않습니다.", null);
            }
            return new StoredImage(bytes, contentType);
        } catch (LimitedBodyReader.BodyTooLargeException exception) {
            throw new RecommendationException(HttpStatus.PAYLOAD_TOO_LARGE, "CLOTHES_IMAGE_TOO_LARGE",
                    "옷 이미지 크기가 허용 범위를 초과했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RecommendationException(HttpStatus.BAD_GATEWAY, "CLOTHES_IMAGE_FETCH_INTERRUPTED",
                    "옷 이미지 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new RecommendationException(HttpStatus.BAD_GATEWAY, "CLOTHES_IMAGE_FETCH_FAILED",
                    "저장된 옷 이미지를 불러오지 못했습니다.", exception);
        }
    }

    @Override
    public StoredObject upload(UUID userId, byte[] png) {
        validateConfiguration();
        if (png.length > maxImageBytes) {
            throw new RecommendationException(HttpStatus.PAYLOAD_TOO_LARGE, "RECOMMENDATION_IMAGE_TOO_LARGE",
                    "생성된 추천 이미지 크기가 허용 범위를 초과했습니다.");
        }
        String objectPath = "recommendations/" + userId + "/" + UUID.randomUUID() + ".png";
        URI uploadUri = objectUri(objectPath);
        try {
            restClient.put()
                    .uri(uploadUri)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .header("x-upsert", "false")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(png)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            throw new RecommendationException(HttpStatus.BAD_GATEWAY, "RECOMMENDATION_IMAGE_UPLOAD_FAILED",
                    "추천 이미지를 Supabase Storage에 저장하지 못했습니다.", exception);
        }
        return new StoredObject(
                supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + objectPath,
                objectPath
        );
    }

    @Override
    public void deleteQuietly(String objectPath) {
        try {
            restClient.delete()
                    .uri(objectUri(objectPath))
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            log.warn("Failed to clean up recommendation image: path={}, status={}",
                    objectPath, exception.getStatusCode());
        } catch (RuntimeException exception) {
            log.warn("Failed to clean up recommendation image: path={}", objectPath, exception);
        }
    }

    private URI objectUri(String objectPath) {
        return URI.create(supabaseUrl + "/storage/v1/object/" + bucket + "/" + objectPath);
    }

    private void validateConfiguration() {
        if (supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank()) {
            throw new RecommendationException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_CONFIGURATION_MISSING",
                    "Supabase Storage 서버 설정이 필요합니다.");
        }
    }

    private RecommendationException invalidStoredImage(String message, Throwable cause) {
        return new RecommendationException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_CLOTHES_IMAGE", message, cause);
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

    private String removeTrailingSlash(String value) {
        if (value == null) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

}
