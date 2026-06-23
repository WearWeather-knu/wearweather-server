package wearweather.wearweather_server.presentation.clothes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;

public record ClothesImportRequest(
        @NotBlank String analysisToken,
        @NotBlank @Size(max = 100) String name,
        @NotNull ClothesCategory category,
        @NotNull Float minTemp,
        @NotNull Float maxTemp,
        @NotNull @Valid ClothesDetailsPayload details,
        Boolean acceptCategoryMismatch
) {
}
