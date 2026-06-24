package wearweather.wearweather_server.application.gemini.dto;

import java.util.List;
import wearweather.wearweather_server.domain.clothes.type.ClothesCategory;

public record OutfitImageRecommendationResponse(
        Long recommendationId,
        String description,
        String imageUrl,
        List<Long> usedClothesIds,
        List<ClothesCategory> missingCategories
) {
}
