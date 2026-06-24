package wearweather.wearweather_server.application.gemini;

import wearweather.wearweather_server.domain.clothes.ClothesCategory;

import java.util.Map;

public record OutfitCandidate(
        Long id,
        String name,
        ClothesCategory category,
        Float minTemp,
        Float maxTemp,
        Map<String, Object> attributes
) {
}
