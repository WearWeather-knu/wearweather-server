package wearweather.wearweather_server.presentation.clothes.dto;

import wearweather.wearweather_server.domain.clothes.ClothesCategory;

public record ClothesImportResponse(
        Long clothesId,
        ClothesCategory category,
        String imageUrl,
        boolean productCreated,
        boolean closetLinked
) {
}
