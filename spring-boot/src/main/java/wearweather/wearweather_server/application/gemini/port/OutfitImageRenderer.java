package wearweather.wearweather_server.application.gemini.port;

import java.util.List;

public interface OutfitImageRenderer {
    byte[] render(List<RecommendationImageStoragePort.StoredImage> images);
}
