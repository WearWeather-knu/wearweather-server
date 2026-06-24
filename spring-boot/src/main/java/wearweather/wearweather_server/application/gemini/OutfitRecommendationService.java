package wearweather.wearweather_server.application.gemini;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import wearweather.wearweather_server.application.gemini.RecommendationImageStorageClient.StoredImage;
import wearweather.wearweather_server.application.gemini.RecommendationImageStorageClient.StoredObject;
import wearweather.wearweather_server.application.gemini.dto.OutfitImageRecommendationRequest;
import wearweather.wearweather_server.application.gemini.dto.OutfitImageRecommendationResponse;
import wearweather.wearweather_server.application.user.dto.UserResult;
import wearweather.wearweather_server.domain.clothes.Clothes;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;
import wearweather.wearweather_server.domain.clothes.ClothesJpaRepository;
import wearweather.wearweather_server.domain.recommendation.Recommendation;
import wearweather.wearweather_server.domain.recommendation.RecommendationJpaRepository;
import wearweather.wearweather_server.domain.weather.WeatherLog;
import wearweather.wearweather_server.domain.weather.WeatherLogJpaRepository;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OutfitRecommendationService {
    private static final List<ClothesCategory> CORE_CATEGORIES = List.of(
            ClothesCategory.TOP, ClothesCategory.BOTTOM, ClothesCategory.SHOES
    );

    private final WeatherLogJpaRepository weatherRepository;
    private final ClothesJpaRepository clothesRepository;
    private final RecommendationJpaRepository recommendationRepository;
    private final OutfitCandidateAssembler candidateAssembler;
    private final GeminiOutfitSelectionClient selectionClient;
    private final RecommendationImageStorageClient imageStorageClient;
    private final OutfitBoardRenderer boardRenderer;

    public OutfitImageRecommendationResponse recommend(
            UserResult user,
            OutfitImageRecommendationRequest request
    ) {
        WeatherLog weather = weatherRepository.findById(request.weatherId())
                .orElseThrow(() -> new RecommendationException(HttpStatus.NOT_FOUND, "WEATHER_NOT_FOUND",
                        "요청한 날씨 정보를 찾을 수 없습니다."));

        List<Clothes> ownedClothes = clothesRepository.findActiveByUserId(user.id());
        if (ownedClothes.isEmpty()) {
            throw new RecommendationException(HttpStatus.UNPROCESSABLE_ENTITY, "CLOSET_EMPTY",
                    "등록된 옷이 없어 코디를 추천할 수 없습니다.");
        }
        List<Clothes> candidates = ownedClothes.stream()
                .filter(item -> item.getImageUrl() != null && !item.getImageUrl().isBlank())
                .toList();
        if (candidates.isEmpty()) {
            throw new RecommendationException(HttpStatus.UNPROCESSABLE_ENTITY, "CLOSET_HAS_NO_IMAGES",
                    "이미지가 등록된 옷이 없어 코디 이미지를 만들 수 없습니다.");
        }

        OutfitSelection selection = selectionClient.select(
                weather, user, request.style(), candidateAssembler.assemble(candidates)
        );
        Map<Long, Clothes> candidateById = new HashMap<>();
        candidates.forEach(item -> candidateById.put(item.getId(), item));
        validateSelection(selection, candidateById, candidates);

        List<Long> selectedIds = selection.allIds();
        List<StoredImage> sourceImages = selectedIds.stream()
                .map(candidateById::get)
                .map(Clothes::getImageUrl)
                .map(imageStorageClient::download)
                .toList();
        byte[] png = boardRenderer.render(sourceImages);
        StoredObject storedObject = imageStorageClient.upload(user.id(), png);

        Recommendation recommendation;
        try {
            recommendation = recommendationRepository.saveAndFlush(new Recommendation(
                    user.id(), weather.getId(), selection.description(), selection.topIds(),
                    selection.bottomId(), selection.outerId(), selection.shoesId(),
                    selection.accIds(), selection.bagId(), storedObject.publicUrl()
            ));
        } catch (RuntimeException exception) {
            imageStorageClient.deleteQuietly(storedObject.objectPath());
            throw new RecommendationException(HttpStatus.INTERNAL_SERVER_ERROR, "RECOMMENDATION_SAVE_FAILED",
                    "추천 이력을 저장하지 못했습니다.", exception);
        }

        return new OutfitImageRecommendationResponse(
                recommendation.getId(), selection.description(), storedObject.publicUrl(),
                selectedIds, missingCoreCategories(candidates)
        );
    }

    private void validateSelection(OutfitSelection selection, Map<Long, Clothes> candidatesById,
                                   List<Clothes> candidates) {
        List<Long> allIds = selection.allIds();
        if (allIds.isEmpty()) {
            throw invalidSelection("선택된 옷이 없습니다.");
        }
        if (new HashSet<>(allIds).size() != allIds.size()) {
            throw invalidSelection("동일한 옷이 중복 선택되었습니다.");
        }
        for (Long id : allIds) {
            if (!candidatesById.containsKey(id)) {
                throw invalidSelection("사용자 옷장에 없는 옷이 선택되었습니다.");
            }
        }

        validateCategory(selection.topIds(), ClothesCategory.TOP, candidatesById);
        validateCategory(single(selection.bottomId()), ClothesCategory.BOTTOM, candidatesById);
        validateCategory(single(selection.outerId()), ClothesCategory.OUTER, candidatesById);
        validateCategory(single(selection.shoesId()), ClothesCategory.SHOES, candidatesById);
        validateCategory(selection.accIds(), ClothesCategory.ACC, candidatesById);
        validateCategory(single(selection.bagId()), ClothesCategory.BAG, candidatesById);

        Set<ClothesCategory> available = EnumSet.noneOf(ClothesCategory.class);
        candidates.forEach(item -> available.add(item.getCategory()));
        for (ClothesCategory category : CORE_CATEGORIES) {
            if (available.contains(category) && !containsCategory(allIds, category, candidatesById)) {
                throw invalidSelection("필수 카테고리의 옷이 선택되지 않았습니다: " + category);
            }
        }
    }

    private void validateCategory(List<Long> ids, ClothesCategory expected, Map<Long, Clothes> candidatesById) {
        for (Long id : ids) {
            if (candidatesById.get(id).getCategory() != expected) {
                throw invalidSelection("선택된 옷의 카테고리가 응답 필드와 일치하지 않습니다.");
            }
        }
    }

    private boolean containsCategory(List<Long> ids, ClothesCategory category, Map<Long, Clothes> candidatesById) {
        return ids.stream().map(candidatesById::get).anyMatch(item -> item.getCategory() == category);
    }

    private List<ClothesCategory> missingCoreCategories(List<Clothes> candidates) {
        Set<ClothesCategory> available = EnumSet.noneOf(ClothesCategory.class);
        candidates.forEach(item -> available.add(item.getCategory()));
        List<ClothesCategory> missing = new ArrayList<>();
        CORE_CATEGORIES.stream().filter(category -> !available.contains(category)).forEach(missing::add);
        return List.copyOf(missing);
    }

    private List<Long> single(Long id) {
        return id == null ? List.of() : List.of(id);
    }

    private RecommendationException invalidSelection(String message) {
        return new RecommendationException(HttpStatus.BAD_GATEWAY, "INVALID_OUTFIT_SELECTION", message);
    }
}
