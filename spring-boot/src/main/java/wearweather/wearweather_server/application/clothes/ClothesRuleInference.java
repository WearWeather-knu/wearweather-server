package wearweather.wearweather_server.application.clothes;

import org.springframework.stereotype.Component;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;
import wearweather.wearweather_server.application.clothes.dto.ClothesDetailsPayload;

import java.util.Locale;

@Component
public class ClothesRuleInference {
    public ClothesInferenceResult infer(MusinsaProduct product, ClothesCategory category) {
        String text = (product.name() + " " + product.description()).toLowerCase(Locale.ROOT);
        Float minTemp = null;
        Float maxTemp = null;
        String thickness = null;

        if (containsAny(text, "패딩", "다운", "heavy", "winter", "기모")) {
            minTemp = -15f;
            maxTemp = 8f;
            thickness = "THICK";
        } else if (containsAny(text, "반팔", "민소매", "sleeveless", "short sleeve", "여름", "쿨")) {
            minTemp = 22f;
            maxTemp = 35f;
            thickness = "THIN";
        }

        String color = inferColor(text);
        String material = inferMaterial(text);
        boolean waterproof = containsAny(text, "방수", "waterproof", "고어텍스", "gore-tex", "gtx");
        boolean windproof = containsAny(text, "방풍", "windproof", "윈드브레이커");

        ClothesDetailsPayload details = switch (category) {
            case TOP -> new ClothesDetailsPayload(
                    inferSleeve(text), thickness, inferFit(text), material, color,
                    null, null, null, null, null
            );
            case OUTER -> new ClothesDetailsPayload(
                    null, thickness, inferFit(text), material, color,
                    null, null, windproof, waterproof, null
            );
            case BOTTOM -> new ClothesDetailsPayload(
                    null, null, inferFit(text), material, color,
                    inferBottomLength(text), null, null, null, null
            );
            case ACC -> new ClothesDetailsPayload(
                    null, null, null, null, color,
                    null, inferAccessoryType(text), null, null, inferWarmthBonus(text)
            );
            case SHOES -> new ClothesDetailsPayload(
                    null, null, null, material, color,
                    null, inferShoeType(text), null, waterproof, null
            );
            case BAG -> new ClothesDetailsPayload(
                    null, null, null, material, color,
                    null, inferBagType(text), null, waterproof, null
            );
        };
        return new ClothesInferenceResult(minTemp, maxTemp, details);
    }

    private String inferColor(String text) {
        if (containsAny(text, "블랙", "black")) return "BLACK";
        if (containsAny(text, "화이트", "white", "아이보리", "ivory")) return "WHITE";
        if (containsAny(text, "그레이", "gray", "grey")) return "GRAY";
        if (containsAny(text, "베이지", "beige")) return "BEIGE";
        if (containsAny(text, "브라운", "brown")) return "BROWN";
        if (containsAny(text, "네이비", "navy")) return "NAVY";
        if (containsAny(text, "블루", "blue")) return "BLUE";
        if (containsAny(text, "그린", "green")) return "GREEN";
        if (containsAny(text, "레드", "red")) return "RED";
        if (containsAny(text, "핑크", "pink")) return "PINK";
        if (containsAny(text, "퍼플", "purple")) return "PURPLE";
        return null;
    }

    private String inferMaterial(String text) {
        if (containsAny(text, "면 ", "코튼", "cotton")) return "COTTON";
        if (containsAny(text, "폴리에스터", "polyester")) return "POLYESTER";
        if (containsAny(text, "나일론", "nylon")) return "NYLON";
        if (containsAny(text, "캐시미어", "cashmere")) return "CASHMERE";
        if (containsAny(text, "울 ", "모 ", "wool")) return "WOOL";
        if (containsAny(text, "리넨", "린넨", "linen")) return "LINEN";
        if (containsAny(text, "데님", "denim")) return "DENIM";
        if (containsAny(text, "스웨이드", "suede")) return "SUEDE";
        if (containsAny(text, "가죽", "레더", "leather")) return "LEATHER";
        if (containsAny(text, "다운", "down")) return "DOWN";
        if (containsAny(text, "플리스", "fleece")) return "FLEECE";
        return null;
    }

    private String inferSleeve(String text) {
        if (containsAny(text, "민소매", "sleeveless")) return "SLEEVELESS";
        if (containsAny(text, "반팔", "short sleeve")) return "SHORT";
        if (containsAny(text, "7부", "three quarter")) return "THREE_QUARTER";
        if (containsAny(text, "긴팔", "long sleeve")) return "LONG";
        return null;
    }

    private String inferFit(String text) {
        if (containsAny(text, "슬림", "slim")) return "SLIM";
        if (containsAny(text, "오버사이즈", "오버핏", "oversized")) return "OVERSIZED";
        if (containsAny(text, "루즈", "loose", "와이드")) return "LOOSE";
        if (containsAny(text, "레귤러", "regular")) return "REGULAR";
        return null;
    }

    private String inferBottomLength(String text) {
        if (containsAny(text, "쇼츠", "반바지", "shorts")) return "SHORT";
        if (containsAny(text, "크롭", "cropped")) return "CROPPED";
        if (containsAny(text, "롱", "long")) return "LONG";
        return null;
    }

    private String inferAccessoryType(String text) {
        if (containsAny(text, "비니", "beanie")) return "BEANIE";
        if (containsAny(text, "캡", "cap")) return "CAP";
        if (containsAny(text, "모자", "hat")) return "HAT";
        if (containsAny(text, "스카프", "머플러", "scarf")) return "SCARF";
        if (containsAny(text, "장갑", "gloves")) return "GLOVES";
        if (containsAny(text, "벨트", "belt")) return "BELT";
        return null;
    }

    private Integer inferWarmthBonus(String text) {
        if (containsAny(text, "장갑", "머플러", "스카프", "기모", "울", "캐시미어")) return 2;
        return 0;
    }

    private String inferShoeType(String text) {
        if (containsAny(text, "스니커즈", "운동화", "sneakers")) return "SNEAKERS";
        if (containsAny(text, "부츠", "boots")) return "BOOTS";
        if (containsAny(text, "로퍼", "loafers")) return "LOAFERS";
        if (containsAny(text, "샌들", "sandals")) return "SANDALS";
        if (containsAny(text, "슬리퍼", "slippers")) return "SLIPPERS";
        if (containsAny(text, "구두", "dress shoes")) return "DRESS_SHOES";
        return null;
    }

    private String inferBagType(String text) {
        if (containsAny(text, "백팩", "backpack")) return "BACKPACK";
        if (containsAny(text, "토트", "tote")) return "TOTE";
        if (containsAny(text, "크로스", "crossbody")) return "CROSSBODY";
        if (containsAny(text, "숄더", "shoulder")) return "SHOULDER";
        if (containsAny(text, "클러치", "clutch")) return "CLUTCH";
        if (containsAny(text, "더플", "duffel")) return "DUFFEL";
        return null;
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }
}
