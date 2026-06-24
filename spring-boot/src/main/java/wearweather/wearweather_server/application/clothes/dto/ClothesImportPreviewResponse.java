package wearweather.wearweather_server.application.clothes.dto;

import wearweather.wearweather_server.domain.clothes.ClothesCategory;

import java.util.List;
import java.util.Map;

public record ClothesImportPreviewResponse(
        String analysisToken,
        String canonicalUrl,
        ClothesCategory requestedCategory,
        ClothesCategory detectedCategory,
        boolean categoryMismatch,
        boolean existingProduct,
        CommonFields common,
        ClothesDetailsPayload details,
        Map<String, FieldSource> fieldSources,
        List<String> warnings
) {
    public record CommonFields(
            String name,
            String imagePreviewUrl,
            Float minTemp,
            Float maxTemp
    ) {
    }

    public enum FieldSource {
        EXTRACTED,
        INFERRED,
        USER_REQUIRED
    }
}
