package wearweather.wearweather_server.application.gemini;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import wearweather.wearweather_server.application.user.dto.UserResult;
import wearweather.wearweather_server.application.gemini.port.OutfitImageRenderer;
import wearweather.wearweather_server.application.gemini.port.OutfitSelectionPort;
import wearweather.wearweather_server.application.gemini.port.RecommendationImageStoragePort;
import wearweather.wearweather_server.application.gemini.port.RecommendationImageStoragePort.StoredImage;
import wearweather.wearweather_server.application.gemini.port.RecommendationImageStoragePort.StoredObject;
import wearweather.wearweather_server.domain.clothes.Clothes;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;
import wearweather.wearweather_server.domain.clothes.ClothesJpaRepository;
import wearweather.wearweather_server.domain.recommendation.Recommendation;
import wearweather.wearweather_server.domain.recommendation.RecommendationJpaRepository;
import wearweather.wearweather_server.domain.weather.WeatherLog;
import wearweather.wearweather_server.domain.weather.WeatherLogJpaRepository;
import wearweather.wearweather_server.application.gemini.dto.OutfitImageRecommendationRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutfitRecommendationServiceTests {
    @Mock WeatherLogJpaRepository weatherRepository;
    @Mock ClothesJpaRepository clothesRepository;
    @Mock RecommendationJpaRepository recommendationRepository;
    @Mock OutfitCandidateAssembler candidateAssembler;
    @Mock OutfitSelectionPort selectionPort;
    @Mock RecommendationImageStoragePort imageStoragePort;
    @Mock OutfitImageRenderer imageRenderer;

    private OutfitRecommendationService service;
    private UserResult user;
    private WeatherLog weather;

    @BeforeEach
    void setUp() {
        service = new OutfitRecommendationService(
                weatherRepository, clothesRepository, recommendationRepository, candidateAssembler,
                selectionPort, imageStoragePort, imageRenderer
        );
        user = new UserResult(UUID.randomUUID(), "user@example.com", "tester", 25,
                null, 0.0f, null);
        weather = org.mockito.Mockito.mock(WeatherLog.class);
        org.mockito.Mockito.lenient().when(weather.getId()).thenReturn(7L);
    }

    @Test
    void rejectsUnknownWeatherBeforeReadingCloset() {
        when(weatherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recommend(user, new OutfitImageRecommendationRequest(999L, null)))
                .isInstanceOfSatisfying(RecommendationException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.code()).isEqualTo("WEATHER_NOT_FOUND");
                });
        verify(clothesRepository, never()).findActiveByUserId(any());
    }

    @Test
    void rejectsSelectionContainingClothesOutsideUsersCloset() {
        Clothes top = clothes(1L, ClothesCategory.TOP);
        when(weatherRepository.findById(7L)).thenReturn(Optional.of(weather));
        when(clothesRepository.findActiveByUserId(user.id())).thenReturn(List.of(top));
        when(candidateAssembler.assemble(List.of(top))).thenReturn(List.of());
        when(selectionPort.select(weather, user, null, List.of()))
                .thenReturn(new OutfitSelection("설명", List.of(999L), null, null, null, List.of(), null));

        assertThatThrownBy(() -> service.recommend(user, new OutfitImageRecommendationRequest(7L, null)))
                .isInstanceOfSatisfying(RecommendationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("INVALID_OUTFIT_SELECTION"));
        verify(imageStoragePort, never()).download(any());
        verify(recommendationRepository, never()).saveAndFlush(any());
    }

    @Test
    void storesImageAndRecommendationHistoryAndReturnsMissingCoreCategory() {
        Clothes top = clothes(1L, ClothesCategory.TOP);
        Clothes bottom = clothes(2L, ClothesCategory.BOTTOM);
        List<Clothes> clothes = List.of(top, bottom);
        OutfitSelection selection = new OutfitSelection(
                "쌀쌀한 날씨에 맞춘 코디", List.of(1L), 2L, null, null, List.of(), null
        );
        var storedImage = new StoredImage(new byte[]{1}, "image/png");
        var storedObject = new StoredObject(
                "https://example.supabase.co/recommendation.png", "recommendations/user/image.png"
        );

        when(weatherRepository.findById(7L)).thenReturn(Optional.of(weather));
        when(clothesRepository.findActiveByUserId(user.id())).thenReturn(clothes);
        when(candidateAssembler.assemble(clothes)).thenReturn(List.of());
        when(selectionPort.select(weather, user, "MINIMAL", List.of())).thenReturn(selection);
        when(imageStoragePort.download(any())).thenReturn(storedImage);
        when(imageRenderer.render(List.of(storedImage, storedImage))).thenReturn(new byte[]{9, 8, 7});
        when(imageStoragePort.upload(user.id(), new byte[]{9, 8, 7})).thenReturn(storedObject);
        when(recommendationRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Recommendation saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 101L);
            return saved;
        });

        var response = service.recommend(user, new OutfitImageRecommendationRequest(7L, "MINIMAL"));

        assertThat(response.recommendationId()).isEqualTo(101L);
        assertThat(response.imageUrl()).isEqualTo(storedObject.publicUrl());
        assertThat(response.usedClothesIds()).containsExactly(1L, 2L);
        assertThat(response.missingCategories()).containsExactly(ClothesCategory.SHOES);

        ArgumentCaptor<Recommendation> captor = ArgumentCaptor.forClass(Recommendation.class);
        verify(recommendationRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getWeatherId()).isEqualTo(7L);
        assertThat(captor.getValue().getTopIds()).containsExactly(1L);
        assertThat(captor.getValue().getBottomId()).isEqualTo(2L);
        assertThat(captor.getValue().getImageUrl()).isEqualTo(storedObject.publicUrl());
    }

    @Test
    void deletesUploadedImageWhenHistorySaveFails() {
        Clothes top = clothes(1L, ClothesCategory.TOP);
        OutfitSelection selection = new OutfitSelection("설명", List.of(1L), null, null, null, List.of(), null);
        var storedImage = new StoredImage(new byte[]{1}, "image/png");
        var storedObject = new StoredObject("https://image", "recommendations/path.png");

        when(weatherRepository.findById(7L)).thenReturn(Optional.of(weather));
        when(clothesRepository.findActiveByUserId(user.id())).thenReturn(List.of(top));
        when(candidateAssembler.assemble(List.of(top))).thenReturn(List.of());
        when(selectionPort.select(weather, user, null, List.of())).thenReturn(selection);
        when(imageStoragePort.download(top.getImageUrl())).thenReturn(storedImage);
        when(imageRenderer.render(List.of(storedImage))).thenReturn(new byte[]{2});
        when(imageStoragePort.upload(user.id(), new byte[]{2})).thenReturn(storedObject);
        when(recommendationRepository.saveAndFlush(any())).thenThrow(new IllegalStateException("db failed"));

        assertThatThrownBy(() -> service.recommend(user, new OutfitImageRecommendationRequest(7L, null)))
                .isInstanceOfSatisfying(RecommendationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("RECOMMENDATION_SAVE_FAILED"));
        verify(imageStoragePort).deleteQuietly(storedObject.objectPath());
    }

    private Clothes clothes(Long id, ClothesCategory category) {
        Clothes value = org.mockito.Mockito.mock(Clothes.class);
        org.mockito.Mockito.lenient().when(value.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(value.getCategory()).thenReturn(category);
        org.mockito.Mockito.lenient().when(value.getImageUrl()).thenReturn("https://example.supabase.co/" + id + ".png");
        return value;
    }
}
