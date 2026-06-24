package wearweather.wearweather_server.application.gemini.port;

import java.util.UUID;

public interface RecommendationImageStoragePort {
    StoredImage download(String imageUrl);

    StoredObject upload(UUID userId, byte[] png);

    void deleteQuietly(String objectPath);

    record StoredImage(byte[] bytes, String contentType) {
    }

    record StoredObject(String publicUrl, String objectPath) {
    }
}
