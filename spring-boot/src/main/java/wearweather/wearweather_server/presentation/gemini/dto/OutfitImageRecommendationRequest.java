package wearweather.wearweather_server.presentation.gemini.dto;

import java.util.Map;

public record OutfitImageRecommendationRequest(
        Map<String, Object> weather,
        Integer outfitCount,
        String style
) {
}
