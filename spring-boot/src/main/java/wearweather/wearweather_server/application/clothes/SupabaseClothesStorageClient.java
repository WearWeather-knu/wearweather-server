package wearweather.wearweather_server.application.clothes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class SupabaseClothesStorageClient {
    private final RestClient restClient;
    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucket;

    public SupabaseClothesStorageClient(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.service-role-key:}") String serviceRoleKey,
            @Value("${supabase.storage.clothes-bucket:clothes-images}") String bucket,
            @Value("${app.clothes-import.connect-timeout-seconds:3}") long connectTimeoutSeconds,
            @Value("${app.clothes-import.request-timeout-seconds:8}") long requestTimeoutSeconds
    ) {
        this.supabaseUrl = removeTrailingSlash(supabaseUrl);
        this.serviceRoleKey = serviceRoleKey;
        this.bucket = bucket;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(requestTimeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public String uploadMusinsaProduct(String productId, RemoteImage image) {
        validateConfiguration();
        String objectPath = "products/musinsa/" + productId + "." + image.extension();
        URI uploadUri = URI.create(supabaseUrl + "/storage/v1/object/" + bucket + "/" + objectPath);
        try {
            restClient.put()
                    .uri(uploadUri)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(image.contentType()))
                    .body(image.bytes())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new ClothesImportException(HttpStatus.BAD_GATEWAY, "STORAGE_UPLOAD_FAILED",
                    "상품 이미지를 Supabase Storage에 저장하지 못했습니다.", exception);
        } catch (RuntimeException exception) {
            throw new ClothesImportException(HttpStatus.BAD_GATEWAY, "STORAGE_UPLOAD_FAILED",
                    "상품 이미지를 Supabase Storage에 저장하지 못했습니다.", exception);
        }
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + objectPath;
    }

    private void validateConfiguration() {
        if (supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank()) {
            throw new ClothesImportException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_CONFIGURATION_MISSING",
                    "Supabase Storage 서버 설정이 필요합니다.");
        }
    }

    private String removeTrailingSlash(String value) {
        if (value == null) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
