package wearweather.wearweather_server.application.clothes;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.clothes.ClothesImportTokenService.TokenPayload;
import wearweather.wearweather_server.application.clothes.dto.ClothesDetailsPayload;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportPreviewRequest;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportPreviewResponse;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportPreviewResponse.FieldSource;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportRequest;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportResponse;
import wearweather.wearweather_server.application.clothes.port.ClothesImageStoragePort;
import wearweather.wearweather_server.application.clothes.port.ClothesInferencePort;
import wearweather.wearweather_server.application.clothes.port.ProductImportPort;
import wearweather.wearweather_server.application.clothes.port.RemoteImagePort;
import wearweather.wearweather_server.application.user.UserService;
import wearweather.wearweather_server.domain.clothes.entity.Clothes;
import wearweather.wearweather_server.domain.clothes.repository.ClothesJpaRepository;
import wearweather.wearweather_server.domain.clothes.type.ClothesCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClothesImportService {
    private static final Logger log = LoggerFactory.getLogger(ClothesImportService.class);

    private final ProductImportPort productImportPort;
    private final RemoteImagePort remoteImagePort;
    private final ClothesRuleInference ruleInference;
    private final ClothesInferencePort clothesInferencePort;
    private final ClothesImportTokenService tokenService;
    private final ClothesImageStoragePort imageStoragePort;
    private final ClothesDraftValidator validator;
    private final ClothesPersistenceService persistenceService;
    private final ClothesJpaRepository clothesRepository;
    private final UserService userService;

    public ClothesImportPreviewResponse preview(AuthenticatedUser user, ClothesImportPreviewRequest request) {
        long startedAt = System.nanoTime();
        MusinsaProduct product = productImportPort.fetch(request.originalUrl());
        boolean existing = clothesRepository.findByOriginalUrl(product.canonicalUrl()).isPresent();
        List<String> warnings = new ArrayList<>();

        RemoteImage image = null;
        try {
            image = remoteImagePort.download(product.imageUrl());
        } catch (ClothesImportException exception) {
            warnings.add("대표 이미지를 분석하지 못했습니다. 저장 시 다시 확인합니다.");
        }

        ClothesInferenceResult ruleResult = ruleInference.infer(product, request.category());
        ClothesInferenceResult geminiResult = null;
        try {
            geminiResult = clothesInferencePort.infer(product, request.category(), image);
        } catch (ClothesImportException exception) {
            log.warn("Clothes inference degraded: productId={}, code={}", product.productId(), exception.code());
            warnings.add("AI 추론을 완료하지 못했습니다. 누락된 필드를 직접 입력해주세요.");
        }

        ClothesInferenceResult merged = merge(ruleResult, geminiResult);
        boolean mismatch = product.detectedCategory() != null && product.detectedCategory() != request.category();
        if (mismatch) warnings.add("선택한 카테고리와 무신사 상품 분류가 다릅니다.");

        Map<String, FieldSource> sources = fieldSources(merged);
        String token = tokenService.create(user.id(), product);
        log.info("Clothes preview completed: productId={}, category={}, detectedCategory={}, existing={}, durationMs={}",
                product.productId(), request.category(), product.detectedCategory(), existing,
                (System.nanoTime() - startedAt) / 1_000_000);

        return new ClothesImportPreviewResponse(
                token,
                product.canonicalUrl(),
                request.category(),
                product.detectedCategory(),
                mismatch,
                existing,
                new ClothesImportPreviewResponse.CommonFields(
                        truncate(product.name(), 100), product.imageUrl(), merged.minTemp(), merged.maxTemp()
                ),
                merged.details(),
                sources,
                List.copyOf(warnings)
        );
    }

    public ClothesImportResponse save(AuthenticatedUser user, ClothesImportRequest request) {
        TokenPayload token = tokenService.verify(request.analysisToken(), user.id());
        if (token.detectedCategory() != null && token.detectedCategory() != request.category()
                && !Boolean.TRUE.equals(request.acceptCategoryMismatch())) {
            throw new ClothesImportException(HttpStatus.CONFLICT, "CATEGORY_MISMATCH_CONFIRMATION_REQUIRED",
                    "카테고리 불일치를 확인해야 저장할 수 있습니다.");
        }

        ClothesDetailsPayload details = validator.validateAndNormalize(request);
        userService.getMe(user);

        Clothes existing = clothesRepository.findByOriginalUrl(token.canonicalUrl()).orElse(null);
        if (existing != null) return response(persistenceService.linkExisting(user.id(), existing));

        RemoteImage image = remoteImagePort.download(token.imageUrl());
        String storedImageUrl = imageStoragePort.uploadMusinsaProduct(token.productId(), image);
        try {
            return response(persistenceService.createAndLink(
                    user.id(), request, details, token.canonicalUrl(), storedImageUrl
            ));
        } catch (DataIntegrityViolationException exception) {
            Clothes raced = clothesRepository.findByOriginalUrl(token.canonicalUrl()).orElse(null);
            if (raced == null) throw exception;
            return response(persistenceService.linkExisting(user.id(), raced));
        }
    }

    private ClothesImportResponse response(ClothesPersistenceService.PersistenceResult result) {
        return new ClothesImportResponse(
                result.clothes().getId(), result.clothes().getCategory(), result.clothes().getImageUrl(),
                result.productCreated(), result.closetLinked()
        );
    }

    private ClothesInferenceResult merge(ClothesInferenceResult rules, ClothesInferenceResult ai) {
        if (ai == null) return rules;
        ClothesDetailsPayload first = rules.details();
        ClothesDetailsPayload second = ai.details();
        return new ClothesInferenceResult(
                first(rules.minTemp(), ai.minTemp()),
                first(rules.maxTemp(), ai.maxTemp()),
                new ClothesDetailsPayload(
                        first(first.sleeveLength(), second.sleeveLength()),
                        first(first.thickness(), second.thickness()),
                        first(first.fit(), second.fit()),
                        first(first.material(), second.material()),
                        first(first.color(), second.color()),
                        first(first.length(), second.length()),
                        first(first.type(), second.type()),
                        first(first.windproof(), second.windproof()),
                        first(first.waterproof(), second.waterproof()),
                        first(first.warmthBonus(), second.warmthBonus())
                )
        );
    }

    private Map<String, FieldSource> fieldSources(ClothesInferenceResult result) {
        Map<String, FieldSource> sources = new LinkedHashMap<>();
        sources.put("name", FieldSource.EXTRACTED);
        sources.put("imagePreviewUrl", FieldSource.EXTRACTED);
        sources.put("minTemp", source(result.minTemp()));
        sources.put("maxTemp", source(result.maxTemp()));
        ClothesDetailsPayload details = result.details();
        sources.put("sleeveLength", source(details.sleeveLength()));
        sources.put("thickness", source(details.thickness()));
        sources.put("fit", source(details.fit()));
        sources.put("material", source(details.material()));
        sources.put("color", source(details.color()));
        sources.put("length", source(details.length()));
        sources.put("type", source(details.type()));
        sources.put("windproof", source(details.windproof()));
        sources.put("waterproof", source(details.waterproof()));
        sources.put("warmthBonus", source(details.warmthBonus()));
        return sources;
    }

    private FieldSource source(Object value) {
        return value == null ? FieldSource.USER_REQUIRED : FieldSource.INFERRED;
    }

    private <T> T first(T first, T second) {
        return first != null ? first : second;
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
