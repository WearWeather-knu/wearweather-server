package wearweather.wearweather_server.application.clothes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.clothes.dto.ClothesDetailsPayload;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportRequest;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;
import wearweather.wearweather_server.domain.user.User;
import wearweather.wearweather_server.domain.user.UserJpaRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ClothesQueryServiceTests {
    @Autowired private ClothesQueryService clothesQueryService;
    @Autowired private ClothesPersistenceService persistenceService;
    @Autowired private UserJpaRepository userRepository;

    @Test
    void returnsOnlyAuthenticatedUsersClothes() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        userRepository.save(new User(ownerId, "owner@example.com"));
        userRepository.save(new User(otherId, "other@example.com"));
        ClothesDetailsPayload details = new ClothesDetailsPayload(
                null, null, null, null, null, null, null, null, null, null
        );

        createClothes(ownerId, "내 상의", ClothesCategory.TOP, "101", details);
        createClothes(otherId, "다른 사용자 하의", ClothesCategory.BOTTOM, "202", details);

        var result = clothesQueryService.getMine(new AuthenticatedUser(ownerId, "owner@example.com"));

        assertThat(result).singleElement().satisfies(clothes -> {
            assertThat(clothes.name()).isEqualTo("내 상의");
            assertThat(clothes.category()).isEqualTo(ClothesCategory.TOP);
            assertThat(clothes.originalUrl()).isEqualTo("https://www.musinsa.com/products/101");
        });
    }

    private void createClothes(UUID userId, String name, ClothesCategory category, String productId,
                               ClothesDetailsPayload details) {
        ClothesImportRequest request = new ClothesImportRequest(
                "token", name, category, -5f, 30f, details, false
        );
        persistenceService.createAndLink(
                userId, request, details,
                "https://www.musinsa.com/products/" + productId,
                "https://storage.example/" + productId + ".webp"
        );
    }
}
