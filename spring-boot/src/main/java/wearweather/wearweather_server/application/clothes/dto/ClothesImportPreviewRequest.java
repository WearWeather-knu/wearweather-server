package wearweather.wearweather_server.application.clothes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import wearweather.wearweather_server.domain.clothes.type.ClothesCategory;

public record ClothesImportPreviewRequest(
        @NotNull ClothesCategory category,
        @NotBlank String originalUrl
) {
}
