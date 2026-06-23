package wearweather.wearweather_server.application.clothes;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClothesImportTokenServiceTests {
    private static final String SECRET = "01234567890123456789012345678901";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-06-23T08:00:00Z");

    @Test
    void createsAndVerifiesUserBoundToken() {
        ClothesImportTokenService service = serviceAt(NOW, 900);
        MusinsaProduct product = product();

        String token = service.create(USER_ID, product);
        ClothesImportTokenService.TokenPayload payload = service.verify(token, USER_ID);

        assertThat(payload.productId()).isEqualTo("123");
        assertThat(payload.detectedCategory()).isEqualTo(ClothesCategory.BAG);
        assertThat(payload.expiresAt()).isEqualTo(NOW.plusSeconds(900).getEpochSecond());
    }

    @Test
    void rejectsTamperedWrongUserAndExpiredTokens() {
        ClothesImportTokenService service = serviceAt(NOW, 10);
        String token = service.create(USER_ID, product());

        assertThatThrownBy(() -> service.verify(token + "x", USER_ID))
                .isInstanceOf(ClothesImportException.class);
        assertThatThrownBy(() -> service.verify(token, UUID.randomUUID()))
                .isInstanceOf(ClothesImportException.class);
        assertThatThrownBy(() -> serviceAt(NOW.plusSeconds(11), 10).verify(token, USER_ID))
                .isInstanceOf(ClothesImportException.class)
                .hasMessageContaining("만료");
    }

    private ClothesImportTokenService serviceAt(Instant instant, long ttl) {
        return new ClothesImportTokenService(new ObjectMapper(), SECRET, ttl, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private MusinsaProduct product() {
        return new MusinsaProduct("123", "https://www.musinsa.com/products/123", "백팩",
                "https://image.msscdn.net/test.webp", "방수 나일론", ClothesCategory.BAG);
    }
}
