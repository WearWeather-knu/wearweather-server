package wearweather.wearweather_server.application.clothes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import wearweather.wearweather_server.domain.clothes.ClothesBagJpaRepository;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;
import wearweather.wearweather_server.domain.clothes.UserClothesId;
import wearweather.wearweather_server.domain.clothes.UserClothesJpaRepository;
import wearweather.wearweather_server.domain.user.User;
import wearweather.wearweather_server.domain.user.UserJpaRepository;
import wearweather.wearweather_server.application.clothes.dto.ClothesDetailsPayload;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ClothesPersistenceServiceTests {
    @Autowired private ClothesPersistenceService persistenceService;
    @Autowired private ClothesBagJpaRepository bagRepository;
    @Autowired private UserClothesJpaRepository userClothesRepository;
    @Autowired private UserJpaRepository userRepository;

    @Test
    void storesBagDetailsAndCreatesIdempotentUserLink() {
        UUID userId = UUID.randomUUID();
        userRepository.save(new User(userId, "test@example.com"));
        ClothesDetailsPayload details = new ClothesDetailsPayload(
                null, null, null, "NYLON", "BLACK", null, "BACKPACK", null, true, null
        );
        ClothesImportRequest request = new ClothesImportRequest(
                "token", "테스트 백팩", ClothesCategory.BAG, -5f, 35f, details, false
        );

        ClothesPersistenceService.PersistenceResult created = persistenceService.createAndLink(
                userId, request, details, "https://www.musinsa.com/products/123", "https://storage.example/123.webp"
        );
        ClothesPersistenceService.PersistenceResult linkedAgain = persistenceService.linkExisting(userId, created.clothes());

        assertThat(created.productCreated()).isTrue();
        assertThat(bagRepository.existsById(created.clothes().getId())).isTrue();
        assertThat(userClothesRepository.existsById(new UserClothesId(userId, created.clothes().getId()))).isTrue();
        assertThat(linkedAgain.closetLinked()).isFalse();
    }

    @Test
    void storesNullForEveryDetailFieldAndCategory() {
        UUID userId = UUID.randomUUID();
        userRepository.save(new User(userId, "nullable@example.com"));
        ClothesDetailsPayload details = new ClothesDetailsPayload(
                null, null, null, null, null, null, null, null, null, null
        );

        int productId = 1;
        for (ClothesCategory category : ClothesCategory.values()) {
            ClothesImportRequest request = new ClothesImportRequest(
                    "token", "nullable-" + category, category, -5f, 35f, details, false
            );

            ClothesPersistenceService.PersistenceResult result = persistenceService.createAndLink(
                    userId, request, details,
                    "https://www.musinsa.com/products/nullable-" + productId++,
                    "https://storage.example/nullable.webp"
            );

            assertThat(result.productCreated()).isTrue();
        }
    }
}
