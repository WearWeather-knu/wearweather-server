package wearweather.wearweather_server.application.clothes;

import org.springframework.http.HttpStatus;

public class ClothesException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ClothesException(HttpStatus status, String code, String message) {
        super(message);
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
