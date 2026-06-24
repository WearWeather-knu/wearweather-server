package wearweather.wearweather_server.application.clothes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.clothes.dto.ClothesDetailsPayload;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportRequest;
import wearweather.wearweather_server.domain.clothes.type.ClothesCategory;
import wearweather.wearweather_server.domain.user.User;
import wearweather.wearweather_server.domain.user.UserJpaRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ClothesQueryServiceTests {
    @Autowired private ClothesQueryService clothesQueryService;
    @Autowired private ClothesFavoriteService clothesFavoriteService;
    @Autowired private ClothesPersistenceService persistenceService;
    @Autowired private UserJpaRepository userRepository;

    @Test
    void returnsOnlyAuthenticatedUsersClothesAndUpdatesFavorite() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        userRepository.save(new User(ownerId, "owner@example.com"));
        userRepository.save(new User(otherId, "other@example.com"));
        ClothesDetailsPayload details = new ClothesDetailsPayload(
                null, null, null, null, null, null, null, null, null, null
        );

        Long ownerClothesId = createClothes(ownerId, "내 상의", ClothesCategory.TOP, "101", details);
        Long otherClothesId = createClothes(otherId, "다른 사용자 하의", ClothesCategory.BOTTOM, "202", details);
        AuthenticatedUser owner = new AuthenticatedUser(ownerId, "owner@example.com");

        var result = clothesQueryService.getMine(owner);

        assertThat(result).singleElement().satisfies(clothes -> {
            assertThat(clothes.name()).isEqualTo("내 상의");
            assertThat(clothes.category()).isEqualTo(ClothesCategory.TOP);
            assertThat(clothes.originalUrl()).isEqualTo("https://www.musinsa.com/products/101");
            assertThat(clothes.favorite()).isFalse();
        });

        clothesFavoriteService.setFavorite(owner, ownerClothesId, true);

        assertThat(clothesQueryService.getMine(owner)).singleElement()
                .extracting(clothes -> clothes.favorite())
                .isEqualTo(true);
        assertThatThrownBy(() -> clothesFavoriteService.setFavorite(owner, otherClothesId, true))
                .isInstanceOfSatisfying(ClothesException.class,
                        exception -> assertThat(exception.code()).isEqualTo("USER_CLOTHES_NOT_FOUND"));
    }

    private Long createClothes(UUID userId, String name, ClothesCategory category, String productId,
                               ClothesDetailsPayload details) {
        ClothesImportRequest request = new ClothesImportRequest(
                "token", name, category, -5f, 30f, details, false
        );
        return persistenceService.createAndLink(
                userId, request, details,
                "https://www.musinsa.com/products/" + productId,
                "https://storage.example/" + productId + ".webp"
        ).clothes().getId();
    }
}
