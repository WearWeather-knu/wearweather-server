package wearweather.wearweather_server.application.clothes.dto;

public record ClothesFavoriteResponse(
        Long clothesId,
        boolean favorite
) {
}
