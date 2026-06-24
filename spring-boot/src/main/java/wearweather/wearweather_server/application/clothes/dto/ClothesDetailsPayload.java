package wearweather.wearweather_server.application.clothes.dto;

public record ClothesDetailsPayload(
        String sleeveLength,
        String thickness,
        String fit,
        String material,
        String color,
        String length,
        String type,
        Boolean windproof,
        Boolean waterproof,
        Integer warmthBonus
) {
}
