package wearweather.wearweather_server.application.clothes;

import org.springframework.http.HttpStatus;

public class ClothesImportException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ClothesImportException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public ClothesImportException(HttpStatus status, String code, String message, Throwable cause) {
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
