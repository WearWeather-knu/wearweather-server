package wearweather.wearweather_server.presentation.gemini.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OutfitImageRecommendationRequest(
        @NotNull @Positive Long weatherId,
        @Size(max = 100) String style
) {
}
