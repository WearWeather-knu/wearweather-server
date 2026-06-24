package wearweather.wearweather_server.application.gemini;

import org.springframework.http.HttpStatus;

public class RecommendationException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public RecommendationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public RecommendationException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
