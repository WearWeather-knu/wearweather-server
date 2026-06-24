package wearweather.wearweather_server.application.clothes;

import wearweather.wearweather_server.domain.clothes.type.ClothesCategory;

public record MusinsaProduct(
        String productId,
        String canonicalUrl,
        String name,
        String imageUrl,
        String description,
        ClothesCategory detectedCategory
) {
}
