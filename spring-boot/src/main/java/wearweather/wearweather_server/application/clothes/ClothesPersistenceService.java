package wearweather.wearweather_server.application.clothes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wearweather.wearweather_server.domain.clothes.BottomLength;
import wearweather.wearweather_server.domain.clothes.Clothes;
import wearweather.wearweather_server.domain.clothes.ClothesAccessory;
import wearweather.wearweather_server.domain.clothes.ClothesAccessoryJpaRepository;
import wearweather.wearweather_server.domain.clothes.ClothesBag;
import wearweather.wearweather_server.domain.clothes.ClothesBagJpaRepository;
import wearweather.wearweather_server.domain.clothes.ClothesBottom;
import wearweather.wearweather_server.domain.clothes.ClothesBottomJpaRepository;
import wearweather.wearweather_server.domain.clothes.ClothesFit;
import wearweather.wearweather_server.domain.clothes.ClothesJpaRepository;
import wearweather.wearweather_server.domain.clothes.ClothesOuter;
import wearweather.wearweather_server.domain.clothes.ClothesOuterJpaRepository;
import wearweather.wearweather_server.domain.clothes.ClothesShoes;
import wearweather.wearweather_server.domain.clothes.ClothesShoesJpaRepository;
import wearweather.wearweather_server.domain.clothes.ClothesTop;
import wearweather.wearweather_server.domain.clothes.ClothesTopJpaRepository;
import wearweather.wearweather_server.domain.clothes.SleeveLength;
import wearweather.wearweather_server.domain.clothes.Thickness;
import wearweather.wearweather_server.domain.clothes.UserClothes;
import wearweather.wearweather_server.domain.clothes.UserClothesId;
import wearweather.wearweather_server.domain.clothes.UserClothesJpaRepository;
import wearweather.wearweather_server.application.clothes.dto.ClothesDetailsPayload;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClothesPersistenceService {
    private final ClothesJpaRepository clothesRepository;
    private final ClothesTopJpaRepository topRepository;
    private final ClothesOuterJpaRepository outerRepository;
    private final ClothesBottomJpaRepository bottomRepository;
    private final ClothesAccessoryJpaRepository accessoryRepository;
    private final ClothesShoesJpaRepository shoesRepository;
    private final ClothesBagJpaRepository bagRepository;
    private final UserClothesJpaRepository userClothesRepository;

    @Transactional
    public PersistenceResult createAndLink(UUID userId, ClothesImportRequest request,
                                           ClothesDetailsPayload details, String canonicalUrl, String imageUrl) {
        Clothes clothes = clothesRepository.saveAndFlush(new Clothes(
                request.name().trim(), request.category(), imageUrl, canonicalUrl, request.minTemp(), request.maxTemp()
        ));
        saveDetails(clothes, details);
        userClothesRepository.saveAndFlush(new UserClothes(userId, clothes.getId()));
        return new PersistenceResult(clothes, true, true);
    }

    @Transactional
    public PersistenceResult linkExisting(UUID userId, Clothes clothes) {
        UserClothesId id = new UserClothesId(userId, clothes.getId());
        boolean linked = !userClothesRepository.existsById(id);
        if (linked) userClothesRepository.saveAndFlush(new UserClothes(userId, clothes.getId()));
        return new PersistenceResult(clothes, false, linked);
    }

    private void saveDetails(Clothes clothes, ClothesDetailsPayload details) {
        switch (clothes.getCategory()) {
            case TOP -> topRepository.save(new ClothesTop(
                    clothes, enumOrNull(SleeveLength.class, details.sleeveLength()),
                    enumOrNull(Thickness.class, details.thickness()), enumOrNull(ClothesFit.class, details.fit()),
                    details.material(), details.color()
            ));
            case OUTER -> outerRepository.save(new ClothesOuter(
                    clothes, enumOrNull(Thickness.class, details.thickness()), enumOrNull(ClothesFit.class, details.fit()),
                    details.windproof(), details.waterproof(), details.material(), details.color()
            ));
            case BOTTOM -> bottomRepository.save(new ClothesBottom(
                    clothes, enumOrNull(BottomLength.class, details.length()), enumOrNull(ClothesFit.class, details.fit()),
                    details.material(), details.color()
            ));
            case ACC -> accessoryRepository.save(new ClothesAccessory(
                    clothes, details.type(), details.warmthBonus(), details.color()
            ));
            case SHOES -> shoesRepository.save(new ClothesShoes(
                    clothes, details.type(), details.waterproof(), details.material(), details.color()
            ));
            case BAG -> bagRepository.save(new ClothesBag(
                    clothes, details.type(), details.material(), details.color(), details.waterproof()
            ));
        }
    }

    private <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    public record PersistenceResult(Clothes clothes, boolean productCreated, boolean closetLinked) {
    }
}
