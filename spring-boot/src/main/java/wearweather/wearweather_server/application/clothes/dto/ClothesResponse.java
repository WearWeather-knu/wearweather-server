package wearweather.wearweather_server.application.clothes.dto;

import wearweather.wearweather_server.domain.clothes.entity.Clothes;
import wearweather.wearweather_server.domain.clothes.type.ClothesCategory;

public record ClothesResponse(
        Long clothesId,
        String name,
        ClothesCategory category,
        String imageUrl,
        String originalUrl,
        Float minTemp,
        Float maxTemp
) {
    public static ClothesResponse from(Clothes clothes) {
        return new ClothesResponse(
                clothes.getId(), clothes.getName(), clothes.getCategory(), clothes.getImageUrl(),
                clothes.getOriginalUrl(), clothes.getMinTemp(), clothes.getMaxTemp()
        );
    }
}
