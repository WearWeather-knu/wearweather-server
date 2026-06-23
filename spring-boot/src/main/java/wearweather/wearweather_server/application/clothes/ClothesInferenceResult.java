package wearweather.wearweather_server.application.clothes;

import wearweather.wearweather_server.presentation.clothes.dto.ClothesDetailsPayload;

public record ClothesInferenceResult(
        Float minTemp,
        Float maxTemp,
        ClothesDetailsPayload details
) {
}
