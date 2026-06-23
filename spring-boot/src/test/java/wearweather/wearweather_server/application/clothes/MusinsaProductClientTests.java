package wearweather.wearweather_server.application.clothes;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MusinsaProductClientTests {
    private final MusinsaProductClient client = new MusinsaProductClient(new ObjectMapper(), 1, 1, 100_000);

    @Test
    void canonicalizesCurrentAndLegacyProductUrls() {
        assertThat(client.canonicalUrl("https://www.musinsa.com/products/4322905?source=test"))
                .isEqualTo("https://www.musinsa.com/products/4322905");
        assertThat(client.canonicalUrl("https://m.musinsa.com/app/goods/4322905"))
                .isEqualTo("https://www.musinsa.com/products/4322905");
    }

    @Test
    void rejectsNonMusinsaAndNonHttpsUrls() {
        assertThatThrownBy(() -> client.canonicalUrl("https://musinsa.com.evil.example/products/1"))
                .isInstanceOf(ClothesImportException.class);
        assertThatThrownBy(() -> client.canonicalUrl("http://www.musinsa.com/products/1"))
                .isInstanceOf(ClothesImportException.class);
    }

    @Test
    void parsesJsonLdOpenGraphAndEmbeddedCategory() {
        String html = """
                <html><head>
                  <meta property="og:title" content="fallback title">
                  <script type="application/ld+json">
                    {"@context":"https://schema.org","@type":"Product","name":"테스트 백팩 블랙",
                     "description":"나일론 방수 백팩","image":["https://image.msscdn.net/test.webp"]}
                  </script>
                  <script type="application/json">{"product":{"categoryName":"백팩"}}</script>
                </head></html>
                """;

        MusinsaProduct product = client.parseHtml("https://www.musinsa.com/products/123", html);

        assertThat(product.productId()).isEqualTo("123");
        assertThat(product.name()).isEqualTo("테스트 백팩 블랙");
        assertThat(product.imageUrl()).isEqualTo("https://image.msscdn.net/test.webp");
        assertThat(product.detectedCategory()).isEqualTo(ClothesCategory.BAG);
    }

    @Test
    void detectsCategoryFromMusinsaOpenGraphDescription() {
        String html = """
                <html><head>
                  <meta property="og:title" content="Beaded Mesh Halter Top - 사이즈 &amp; 후기 | 무신사">
                  <meta property="og:image" content="https://image.msscdn.net/test.jpg">
                  <meta property="og:description" content="제품분류 :상의 &gt; 민소매 티셔츠 브랜드 : TEST">
                </head></html>
                """;

        MusinsaProduct product = client.parseHtml("https://www.musinsa.com/products/4322905", html);

        assertThat(product.name()).isEqualTo("Beaded Mesh Halter Top");
        assertThat(product.detectedCategory()).isEqualTo(ClothesCategory.TOP);
    }
}
