package wearweather.wearweather_server.application.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class SupabaseAuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final String jwtSecret;

    public SupabaseAuthService(@Value("${supabase.jwt-secret:}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public AuthenticatedUser authenticate(String authorizationHeader) {
        String accessToken = extractAccessToken(authorizationHeader);
        Claims claims = parseClaims(accessToken);

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.");
        }
        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.");
        }

        return new AuthenticatedUser(
                parseUserId(subject),
                email
        );
    }

    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다.");
        }


        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    private Claims parseClaims(String accessToken) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Supabase JWT secret 설정이 필요합니다.");
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (IllegalArgumentException | JwtException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.", exception);
        }
    }

    private UUID parseUserId(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.", exception);
        }
    }
}
