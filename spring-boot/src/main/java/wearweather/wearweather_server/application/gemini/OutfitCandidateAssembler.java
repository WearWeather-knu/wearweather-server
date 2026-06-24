package wearweather.wearweather_server.application.gemini;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import wearweather.wearweather_server.domain.clothes.entity.Clothes;
import wearweather.wearweather_server.domain.clothes.entity.ClothesAccessory;
import wearweather.wearweather_server.domain.clothes.entity.ClothesBag;
import wearweather.wearweather_server.domain.clothes.entity.ClothesBottom;
import wearweather.wearweather_server.domain.clothes.entity.ClothesOuter;
import wearweather.wearweather_server.domain.clothes.entity.ClothesShoes;
import wearweather.wearweather_server.domain.clothes.entity.ClothesTop;
import wearweather.wearweather_server.domain.clothes.repository.ClothesAccessoryJpaRepository;
import wearweather.wearweather_server.domain.clothes.repository.ClothesBagJpaRepository;
import wearweather.wearweather_server.domain.clothes.repository.ClothesBottomJpaRepository;
import wearweather.wearweather_server.domain.clothes.repository.ClothesOuterJpaRepository;
import wearweather.wearweather_server.domain.clothes.repository.ClothesShoesJpaRepository;
import wearweather.wearweather_server.domain.clothes.repository.ClothesTopJpaRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class OutfitCandidateAssembler {
    private final ClothesTopJpaRepository topRepository;
    private final ClothesOuterJpaRepository outerRepository;
    private final ClothesBottomJpaRepository bottomRepository;
    private final ClothesAccessoryJpaRepository accessoryRepository;
    private final ClothesShoesJpaRepository shoesRepository;
    private final ClothesBagJpaRepository bagRepository;

    List<OutfitCandidate> assemble(List<Clothes> clothes) {
        List<Long> ids = clothes.stream().map(Clothes::getId).toList();
        Map<Long, ClothesTop> tops = index(topRepository.findAllById(ids), ClothesTop::getClothesId);
        Map<Long, ClothesOuter> outers = index(outerRepository.findAllById(ids), ClothesOuter::getClothesId);
        Map<Long, ClothesBottom> bottoms = index(bottomRepository.findAllById(ids), ClothesBottom::getClothesId);
        Map<Long, ClothesAccessory> accessories = index(accessoryRepository.findAllById(ids), ClothesAccessory::getClothesId);
        Map<Long, ClothesShoes> shoes = index(shoesRepository.findAllById(ids), ClothesShoes::getClothesId);
        Map<Long, ClothesBag> bags = index(bagRepository.findAllById(ids), ClothesBag::getClothesId);

        return clothes.stream().map(item -> new OutfitCandidate(
                item.getId(), item.getName(), item.getCategory(), item.getMinTemp(), item.getMaxTemp(),
                attributes(item, tops, outers, bottoms, accessories, shoes, bags)
        )).toList();
    }

    private Map<String, Object> attributes(
            Clothes item,
            Map<Long, ClothesTop> tops,
            Map<Long, ClothesOuter> outers,
            Map<Long, ClothesBottom> bottoms,
            Map<Long, ClothesAccessory> accessories,
            Map<Long, ClothesShoes> shoes,
            Map<Long, ClothesBag> bags
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        switch (item.getCategory()) {
            case TOP -> {
                ClothesTop value = tops.get(item.getId());
                if (value != null) {
                    put(values, "sleeveLength", value.getSleeveLength());
                    put(values, "thickness", value.getThickness());
                    put(values, "fit", value.getFit());
                    put(values, "material", value.getMaterial());
                    put(values, "color", value.getColor());
                }
            }
            case OUTER -> {
                ClothesOuter value = outers.get(item.getId());
                if (value != null) {
                    put(values, "thickness", value.getThickness());
                    put(values, "fit", value.getFit());
                    put(values, "windproof", value.getWindproof());
                    put(values, "waterproof", value.getWaterproof());
                    put(values, "material", value.getMaterial());
                    put(values, "color", value.getColor());
                }
            }
            case BOTTOM -> {
                ClothesBottom value = bottoms.get(item.getId());
                if (value != null) {
                    put(values, "length", value.getLength());
                    put(values, "fit", value.getFit());
                    put(values, "material", value.getMaterial());
                    put(values, "color", value.getColor());
                }
            }
            case ACC -> {
                ClothesAccessory value = accessories.get(item.getId());
                if (value != null) {
                    put(values, "type", value.getType());
                    put(values, "warmthBonus", value.getWarmthBonus());
                    put(values, "color", value.getColor());
                }
            }
            case SHOES -> {
                ClothesShoes value = shoes.get(item.getId());
                if (value != null) {
                    put(values, "type", value.getType());
                    put(values, "waterproof", value.getWaterproof());
                    put(values, "material", value.getMaterial());
                    put(values, "color", value.getColor());
                }
            }
            case BAG -> {
                ClothesBag value = bags.get(item.getId());
                if (value != null) {
                    put(values, "type", value.getType());
                    put(values, "material", value.getMaterial());
                    put(values, "color", value.getColor());
                    put(values, "waterproof", value.getWaterproof());
                }
            }
        }
        return Map.copyOf(values);
    }

    private <T> Map<Long, T> index(List<T> values, Function<T, Long> id) {
        return values.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private void put(Map<String, Object> values, String key, Object value) {
        if (value != null) values.put(key, value);
    }
}
