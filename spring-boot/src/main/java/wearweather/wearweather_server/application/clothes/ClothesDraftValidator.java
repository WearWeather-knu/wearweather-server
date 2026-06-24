package wearweather.wearweather_server.application.clothes;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import wearweather.wearweather_server.domain.clothes.BottomLength;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;
import wearweather.wearweather_server.domain.clothes.ClothesFit;
import wearweather.wearweather_server.domain.clothes.SleeveLength;
import wearweather.wearweather_server.domain.clothes.Thickness;
import wearweather.wearweather_server.application.clothes.dto.ClothesDetailsPayload;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportRequest;

import java.util.Locale;
import java.util.Set;

@Component
public class ClothesDraftValidator {
    private static final Set<String> MATERIALS = Set.of("COTTON", "POLYESTER", "NYLON", "WOOL", "CASHMERE",
            "LINEN", "DENIM", "LEATHER", "SUEDE", "DOWN", "FLEECE", "SYNTHETIC", "MIXED", "OTHER");
    private static final Set<String> COLORS = Set.of("BLACK", "WHITE", "GRAY", "BEIGE", "BROWN", "NAVY", "BLUE",
            "GREEN", "YELLOW", "ORANGE", "RED", "PINK", "PURPLE", "SILVER", "GOLD", "MULTI", "OTHER");
    private static final Set<String> SHOE_TYPES = Set.of("SNEAKERS", "BOOTS", "LOAFERS", "DRESS_SHOES", "SANDALS", "SLIPPERS", "OTHER");
    private static final Set<String> ACC_TYPES = Set.of("HAT", "CAP", "BEANIE", "SCARF", "GLOVES", "BELT", "JEWELRY", "OTHER");
    private static final Set<String> BAG_TYPES = Set.of("BACKPACK", "TOTE", "SHOULDER", "CROSSBODY", "CLUTCH", "DUFFEL", "OTHER");

    public ClothesDetailsPayload validateAndNormalize(ClothesImportRequest request) {
        validateTemperature(request.minTemp(), request.maxTemp());
        ClothesDetailsPayload value = normalize(request.details());
        switch (request.category()) {
            case TOP -> {
                optionalEnum(value.thickness(), Thickness.class, "thickness");
                optionalEnum(value.sleeveLength(), SleeveLength.class, "sleeveLength");
                optionalEnum(value.fit(), ClothesFit.class, "fit");
                optionalSet(value.material(), MATERIALS, "material");
                optionalSet(value.color(), COLORS, "color");
            }
            case OUTER -> {
                optionalEnum(value.thickness(), Thickness.class, "thickness");
                optionalEnum(value.fit(), ClothesFit.class, "fit");
                optionalSet(value.material(), MATERIALS, "material");
                optionalSet(value.color(), COLORS, "color");
            }
            case BOTTOM -> {
                optionalEnum(value.length(), BottomLength.class, "length");
                optionalEnum(value.fit(), ClothesFit.class, "fit");
                optionalSet(value.material(), MATERIALS, "material");
                optionalSet(value.color(), COLORS, "color");
            }
            case ACC -> {
                optionalSet(value.type(), ACC_TYPES, "type");
                optionalSet(value.color(), COLORS, "color");
                if (value.warmthBonus() != null && (value.warmthBonus() < 0 || value.warmthBonus() > 3)) {
                    invalid("warmthBonus는 0~3이어야 합니다.");
                }
            }
            case SHOES -> {
                optionalSet(value.type(), SHOE_TYPES, "type");
                optionalSet(value.material(), MATERIALS, "material");
                optionalSet(value.color(), COLORS, "color");
            }
            case BAG -> {
                optionalSet(value.type(), BAG_TYPES, "type");
                optionalSet(value.material(), MATERIALS, "material");
                optionalSet(value.color(), COLORS, "color");
            }
        }
        return value;
    }

    private ClothesDetailsPayload normalize(ClothesDetailsPayload value) {
        return new ClothesDetailsPayload(
                code(value.sleeveLength()), code(value.thickness()), code(value.fit()), code(value.material()),
                code(value.color()), code(value.length()), code(value.type()), value.windproof(), value.waterproof(),
                value.warmthBonus()
        );
    }

    private void validateTemperature(Float min, Float max) {
        if (min == null || max == null || !Float.isFinite(min) || !Float.isFinite(max)
                || min < -30 || max > 50 || min > max) {
            invalid("온도 범위는 -30~50도이며 최저 온도는 최고 온도보다 클 수 없습니다.");
        }
    }

    private <E extends Enum<E>> void optionalEnum(String value, Class<E> type, String field) {
        if (value == null) return;
        try {
            Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            invalid(field + " 값이 지원 목록에 없습니다.");
        }
    }

    private void optionalSet(String value, Set<String> allowed, String field) {
        if (value != null && !allowed.contains(value)) invalid(field + " 값이 지원 목록에 없습니다.");
    }

    private String code(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private void invalid(String message) {
        throw new ClothesImportException(HttpStatus.BAD_REQUEST, "INVALID_CLOTHES_FIELDS", message);
    }
}
