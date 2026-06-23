package wearweather.wearweather_server.application.clothes;

import org.junit.jupiter.api.Test;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;
import wearweather.wearweather_server.presentation.clothes.dto.ClothesDetailsPayload;
import wearweather.wearweather_server.presentation.clothes.dto.ClothesImportRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClothesDraftValidatorTests {
    private final ClothesDraftValidator validator = new ClothesDraftValidator();

    @Test
    void normalizesAndValidatesBagFields() {
        ClothesImportRequest request = request(ClothesCategory.BAG, -5f, 35f,
                new ClothesDetailsPayload(null, null, null, "nylon", "black", null,
                        "backpack", null, true, null));

        ClothesDetailsPayload result = validator.validateAndNormalize(request);

        assertThat(result.type()).isEqualTo("BACKPACK");
        assertThat(result.material()).isEqualTo("NYLON");
        assertThat(result.color()).isEqualTo("BLACK");
    }

    @Test
    void rejectsInvalidTemperatureAndRequiredFields() {
        ClothesDetailsPayload details = new ClothesDetailsPayload(null, null, null, null, null,
                null, null, null, null, null);

        assertThatThrownBy(() -> validator.validateAndNormalize(request(ClothesCategory.TOP, 30f, 10f, details)))
                .isInstanceOf(ClothesImportException.class)
                .hasMessageContaining("온도");
        assertThatThrownBy(() -> validator.validateAndNormalize(request(ClothesCategory.TOP, 10f, 30f, details)))
                .isInstanceOf(ClothesImportException.class)
                .hasMessageContaining("thickness");
    }

    private ClothesImportRequest request(ClothesCategory category, Float min, Float max, ClothesDetailsPayload details) {
        return new ClothesImportRequest("token", "name", category, min, max, details, false);
    }
}
