package wearweather.wearweather_server.application.clothes.dto;

import wearweather.wearweather_server.domain.clothes.type.ClothesCategory;

public record ClothesImportResponse(
        Long clothesId,
        ClothesCategory category,
        String imageUrl,
        boolean productCreated,
        boolean closetLinked
) {
}
