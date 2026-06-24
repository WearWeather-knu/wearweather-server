package wearweather.wearweather_server.infrastructure.clothes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

@Component
class GeminiRetryExecutor {
    private static final Logger log = LoggerFactory.getLogger(GeminiRetryExecutor.class);
    private static final int MAX_ALLOWED_ATTEMPTS = 5;

    private final int maxAttempts;
    private final long initialBackoffMillis;

    GeminiRetryExecutor(
            @Value("${app.clothes-import.gemini-max-attempts:3}") int maxAttempts,
            @Value("${app.clothes-import.gemini-retry-backoff-millis:300}") long initialBackoffMillis
    ) {
        this.maxAttempts = Math.max(1, Math.min(maxAttempts, MAX_ALLOWED_ATTEMPTS));
        this.initialBackoffMillis = Math.max(0, initialBackoffMillis);
    }

    <T> T execute(Supplier<T> operation) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (RestClientResponseException exception) {
                if (!isRetryable(exception) || attempt == maxAttempts) {
                    throw exception;
                }

                long delayMillis = initialBackoffMillis * (1L << (attempt - 1));
                log.warn("Gemini request temporarily unavailable: status={}, attempt={}/{}, retryDelayMs={}",
                        exception.getStatusCode(), attempt, maxAttempts, delayMillis);
                sleep(delayMillis);
            }
        }
        throw new IllegalStateException("Gemini retry loop completed without a result");
    }

    private boolean isRetryable(RestClientResponseException exception) {
        return exception.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS
                || exception.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE;
    }

    private void sleep(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RetryInterruptedException(exception);
        }
    }

    private static final class RetryInterruptedException extends RuntimeException {
        private RetryInterruptedException(InterruptedException cause) {
            super(cause);
        }
    }
}
