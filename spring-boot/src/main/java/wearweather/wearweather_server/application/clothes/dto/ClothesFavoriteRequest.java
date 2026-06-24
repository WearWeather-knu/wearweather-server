package wearweather.wearweather_server.application.clothes.dto;

import jakarta.validation.constraints.NotNull;

public record ClothesFavoriteRequest(
        @NotNull Boolean favorite
) {
}
