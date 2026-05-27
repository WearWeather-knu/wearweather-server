package wearweather.wearweather_server.application.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SupabaseAuthService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseAuthService.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ALG_ES256 = "ES256";

    private final RestClient restClient;
    private final Map<String, PublicKey> jwksCache = new ConcurrentHashMap<>();
    private final String supabaseUrl;

    public SupabaseAuthService(@Value("${supabase.url:}") String supabaseUrl) {
        this.restClient = RestClient.builder().build();
        this.supabaseUrl = removeTrailingSlash(supabaseUrl);
    }

    public AuthenticatedUser authenticate(String authorizationHeader) {
        String accessToken = extractAccessToken(authorizationHeader);
        JwtClaims claims = parseAndVerifyClaims(accessToken);

        if (claims.subject() == null || claims.subject().isBlank()) {
            log.warn("Supabase auth failed: missing subject claim");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.");
        }
        if (claims.email() == null || claims.email().isBlank()) {
            log.warn("Supabase auth failed: missing email claim for subject={}", claims.subject());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.");
        }

        return new AuthenticatedUser(
                parseUserId(claims.subject()),
                claims.email()
        );
    }

    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            log.warn("Supabase auth failed: missing or invalid Authorization header");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다.");
        }


        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    private JwtClaims parseAndVerifyClaims(String accessToken) {
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            log.error("Supabase auth failed: SUPABASE_URL is not configured");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Supabase URL 설정이 필요합니다.");
        }

        String[] tokenParts = accessToken.split("\\.");
        if (tokenParts.length != 3) {
            log.warn("Supabase auth failed: token format is invalid, parts={}", tokenParts.length);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.");
        }

        try {
            String headerJson = decodeBase64UrlToString(tokenParts[0]);
            JwtHeader header = new JwtHeader(
                    extractJsonStringValue(headerJson, "alg"),
                    extractJsonStringValue(headerJson, "kid")
            );
            log.debug("Supabase auth token header parsed: alg={}, kid={}", header.alg(), header.kid());

            if (!ALG_ES256.equals(header.alg()) || header.kid() == null || header.kid().isBlank()) {
                log.warn("Supabase auth failed: unsupported or missing token header, alg={}, kidPresent={}",
                        header.alg(), header.kid() != null && !header.kid().isBlank());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.");
            }

            PublicKey publicKey = findPublicKey(header.kid());
            if (!verifyEs256Signature(tokenParts, publicKey)) {
                log.warn("Supabase auth failed: ES256 signature verification failed, kid={}", header.kid());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.");
            }

            String payloadJson = decodeBase64UrlToString(tokenParts[1]);
            validateExpiration(payloadJson);

            JwtClaims claims = new JwtClaims(
                    extractJsonStringValue(payloadJson, "sub"),
                    extractJsonStringValue(payloadJson, "email")
            );
            log.debug("Supabase auth token payload parsed: subjectPresent={}, emailPresent={}",
                    claims.subject() != null && !claims.subject().isBlank(),
                    claims.email() != null && !claims.email().isBlank());
            return claims;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Supabase auth failed: unexpected token verification error", exception);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.", exception);
        }
    }

    private PublicKey findPublicKey(String kid) {
        PublicKey cachedPublicKey = jwksCache.get(kid);
        if (cachedPublicKey != null) {
            log.debug("Supabase JWKS cache hit: kid={}", kid);
            return cachedPublicKey;
        }

        log.debug("Supabase JWKS cache miss: kid={}", kid);
        refreshJwks();
        PublicKey publicKey = jwksCache.get(kid);
        if (publicKey == null) {
            log.warn("Supabase auth failed: matching JWKS key not found, kid={}, cachedKids={}", kid, jwksCache.keySet());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.");
        }
        return publicKey;
    }

    private void refreshJwks() {
        log.debug("Supabase JWKS refresh started: url={}", supabaseUrl);
        JwksResponse response;
        try {
            response = restClient.get()
                    .uri(supabaseUrl + "/auth/v1/.well-known/jwks.json")
                    .retrieve()
                    .body(JwksResponse.class);
        } catch (RestClientResponseException exception) {
            log.error("Supabase JWKS refresh failed: status={}, response={}",
                    exception.getStatusCode(), exception.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase JWKS 조회에 실패했습니다.", exception);
        }

        if (response == null || response.keys() == null || response.keys().isEmpty()) {
            log.error("Supabase JWKS refresh failed: empty response");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase JWKS 응답이 비어있습니다.");
        }

        int cachedKeyCount = 0;
        for (Jwk key : response.keys()) {
            if (ALG_ES256.equals(key.alg()) && key.kid() != null) {
                jwksCache.put(key.kid(), createEcPublicKey(key));
                cachedKeyCount++;
            }
        }
        log.debug("Supabase JWKS refresh completed: receivedKeys={}, cachedEs256Keys={}",
                response.keys().size(), cachedKeyCount);
    }

    private ECPublicKey createEcPublicKey(Jwk jwk) {
        if (!"P-256".equals(jwk.crv()) || jwk.x() == null || jwk.y() == null) {
            log.error("Supabase JWKS key unsupported: kid={}, alg={}, crv={}, hasX={}, hasY={}",
                    jwk.kid(), jwk.alg(), jwk.crv(), jwk.x() != null, jwk.y() != null);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "지원하지 않는 Supabase JWKS 키입니다.");
        }

        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = parameters.getParameterSpec(ECParameterSpec.class);

            ECPoint point = new ECPoint(
                    new BigInteger(1, Base64.getUrlDecoder().decode(jwk.x())),
                    new BigInteger(1, Base64.getUrlDecoder().decode(jwk.y()))
            );

            return (ECPublicKey) KeyFactory.getInstance("EC")
                    .generatePublic(new ECPublicKeySpec(point, parameterSpec));
        } catch (Exception exception) {
            log.error("Supabase JWKS key conversion failed: kid={}", jwk.kid(), exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase JWKS 키 변환에 실패했습니다.", exception);
        }
    }

    private boolean verifyEs256Signature(String[] tokenParts, PublicKey publicKey) {
        byte[] joseSignature = Base64.getUrlDecoder().decode(tokenParts[2]);
        if (joseSignature.length != 64) {
            log.warn("Supabase auth failed: invalid ES256 signature length={}", joseSignature.length);
            return false;
        }

        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(publicKey);
            signature.update((tokenParts[0] + "." + tokenParts[1]).getBytes(StandardCharsets.US_ASCII));
            return signature.verify(convertJoseSignatureToDer(joseSignature));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.", exception);
        }
    }

    private byte[] convertJoseSignatureToDer(byte[] joseSignature) {
        byte[] r = normalizeDerInteger(Arrays.copyOfRange(joseSignature, 0, 32));
        byte[] s = normalizeDerInteger(Arrays.copyOfRange(joseSignature, 32, 64));

        int sequenceLength = 2 + r.length + 2 + s.length;
        byte[] derSignature = new byte[2 + sequenceLength];
        int index = 0;
        derSignature[index++] = 0x30;
        derSignature[index++] = (byte) sequenceLength;
        derSignature[index++] = 0x02;
        derSignature[index++] = (byte) r.length;
        System.arraycopy(r, 0, derSignature, index, r.length);
        index += r.length;
        derSignature[index++] = 0x02;
        derSignature[index++] = (byte) s.length;
        System.arraycopy(s, 0, derSignature, index, s.length);
        return derSignature;
    }

    private byte[] normalizeDerInteger(byte[] value) {
        int firstNonZero = 0;
        while (firstNonZero < value.length - 1 && value[firstNonZero] == 0) {
            firstNonZero++;
        }

        byte[] normalized = Arrays.copyOfRange(value, firstNonZero, value.length);
        if ((normalized[0] & 0x80) == 0) {
            return normalized;
        }

        byte[] positive = new byte[normalized.length + 1];
        System.arraycopy(normalized, 0, positive, 1, normalized.length);
        return positive;
    }

    private void validateExpiration(String payloadJson) {
        Long expiration = extractJsonLongValue(payloadJson, "exp");
        if (expiration == null || expiration <= Instant.now().getEpochSecond()) {
            log.warn("Supabase auth failed: token expired or missing exp, exp={}, now={}",
                    expiration, Instant.now().getEpochSecond());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "만료된 인증 토큰입니다.");
        }
    }

    private String decodeBase64UrlToString(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private String extractJsonStringValue(String json, String key) {
        String keyPattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(keyPattern);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(":", keyIndex + keyPattern.length());
        if (colonIndex < 0) {
            return null;
        }

        int valueStart = json.indexOf("\"", colonIndex + 1);
        if (valueStart < 0) {
            return null;
        }

        int valueEnd = json.indexOf("\"", valueStart + 1);
        if (valueEnd < 0) {
            return null;
        }

        return json.substring(valueStart + 1, valueEnd);
    }

    private Long extractJsonLongValue(String json, String key) {
        String keyPattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(keyPattern);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(":", keyIndex + keyPattern.length());
        if (colonIndex < 0) {
            return null;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
            valueEnd++;
        }

        if (valueStart == valueEnd) {
            return null;
        }

        return Long.parseLong(json.substring(valueStart, valueEnd));
    }

    private String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private UUID parseUserId(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.", exception);
        }
    }

    private record JwtHeader(String alg, String kid) {
    }

    private record JwtClaims(String subject, String email) {
    }

    private record JwksResponse(List<Jwk> keys) {
    }

    private record Jwk(String kty, String kid, String alg, String crv, String x, String y) {
    }
}
