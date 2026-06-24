package wearweather.wearweather_server.application.clothes;

import org.junit.jupiter.api.Test;
import wearweather.wearweather_server.domain.clothes.type.ClothesCategory;

import static org.assertj.core.api.Assertions.assertThat;

class ClothesRuleInferenceTests {
    private final ClothesRuleInference inference = new ClothesRuleInference();

    @Test
    void infersExplicitWaterproofBagPropertiesBeforeAi() {
        MusinsaProduct product = new MusinsaProduct("123", "https://www.musinsa.com/products/123",
                "블랙 나일론 방수 백팩", "https://image.msscdn.net/test.webp", "waterproof nylon backpack",
                ClothesCategory.BAG);

        ClothesInferenceResult result = inference.infer(product, ClothesCategory.BAG);

        assertThat(result.details().type()).isEqualTo("BACKPACK");
        assertThat(result.details().material()).isEqualTo("NYLON");
        assertThat(result.details().color()).isEqualTo("BLACK");
        assertThat(result.details().waterproof()).isTrue();
    }
}
