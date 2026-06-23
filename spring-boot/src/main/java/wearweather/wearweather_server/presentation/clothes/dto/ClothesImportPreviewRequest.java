package wearweather.wearweather_server.presentation.clothes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;

public record ClothesImportPreviewRequest(
        @NotNull ClothesCategory category,
        @NotBlank String originalUrl
) {
}
