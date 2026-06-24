package wearweather.wearweather_server.application.clothes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import wearweather.wearweather_server.domain.clothes.type.ClothesCategory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
public class ClothesImportTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long ttlSeconds;
    private final Clock clock;

    @Autowired
    public ClothesImportTokenService(
            ObjectMapper objectMapper,
            @Value("${app.clothes-import.token-secret:}") String secret,
            @Value("${app.clothes-import.token-ttl-seconds:900}") long ttlSeconds
    ) {
        this(objectMapper, secret, ttlSeconds, Clock.systemUTC());
    }

    ClothesImportTokenService(ObjectMapper objectMapper, String secret, long ttlSeconds, Clock clock) {
        this.objectMapper = objectMapper;
        this.secret = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
        this.clock = clock;
    }

    public String create(UUID userId, MusinsaProduct product) {
        validateSecret();
        long expiresAt = Instant.now(clock).plusSeconds(ttlSeconds).getEpochSecond();
        TokenPayload payload = new TokenPayload(
                userId,
                product.productId(),
                product.canonicalUrl(),
                product.imageUrl(),
                product.detectedCategory(),
                expiresAt
        );
        try {
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));
            String encodedSignature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sign(encodedPayload));
            return encodedPayload + "." + encodedSignature;
        } catch (RuntimeException exception) {
            throw new ClothesImportException(HttpStatus.INTERNAL_SERVER_ERROR, "IMPORT_TOKEN_CREATION_FAILED",
                    "상품 분석 토큰을 생성하지 못했습니다.", exception);
        }
    }

    public TokenPayload verify(String token, UUID expectedUserId) {
        validateSecret();
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.", -1);
            if (parts.length != 2) throw invalidToken();
            byte[] providedSignature = Base64.getUrlDecoder().decode(parts[1]);
            if (!java.security.MessageDigest.isEqual(sign(parts[0]), providedSignature)) throw invalidToken();

            TokenPayload payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[0]), TokenPayload.class);
            if (!expectedUserId.equals(payload.userId())) throw invalidToken();
            if (payload.expiresAt() < Instant.now(clock).getEpochSecond()) {
                throw new ClothesImportException(HttpStatus.UNAUTHORIZED, "IMPORT_TOKEN_EXPIRED",
                        "상품 분석 결과가 만료되었습니다. 링크를 다시 분석해주세요.");
            }
            return payload;
        } catch (ClothesImportException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidToken();
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC initialization failed", exception);
        }
    }

    private void validateSecret() {
        if (secret.length < 32) {
            throw new ClothesImportException(HttpStatus.INTERNAL_SERVER_ERROR, "IMPORT_TOKEN_SECRET_MISSING",
                    "CLOTHES_IMPORT_TOKEN_SECRET은 32바이트 이상이어야 합니다.");
        }
    }

    private ClothesImportException invalidToken() {
        return new ClothesImportException(HttpStatus.UNAUTHORIZED, "INVALID_IMPORT_TOKEN",
                "유효하지 않은 상품 분석 토큰입니다.");
    }

    public record TokenPayload(
            UUID userId,
            String productId,
            String canonicalUrl,
            String imageUrl,
            ClothesCategory detectedCategory,
            long expiresAt
    ) {
    }
}
