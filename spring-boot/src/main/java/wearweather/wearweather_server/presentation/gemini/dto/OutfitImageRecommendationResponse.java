package wearweather.wearweather_server.presentation.gemini.dto;

import java.util.List;

public record OutfitImageRecommendationResponse(
        String description,
        List<GeneratedImage> images
) {

    public record GeneratedImage(
            String mimeType,
            String dataUrl
    ) {
    }
}
