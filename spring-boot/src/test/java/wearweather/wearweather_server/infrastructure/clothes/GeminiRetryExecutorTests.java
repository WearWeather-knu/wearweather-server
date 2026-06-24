package wearweather.wearweather_server.infrastructure.clothes;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiRetryExecutorTests {

    @Test
    void retriesServiceUnavailableUntilRequestSucceeds() {
        GeminiRetryExecutor executor = new GeminiRetryExecutor(3, 0);
        AtomicInteger attempts = new AtomicInteger();
        RestClientResponseException unavailable = responseException(HttpStatus.SERVICE_UNAVAILABLE);

        String result = executor.execute(() -> {
            if (attempts.incrementAndGet() < 3) throw unavailable;
            return "success";
        });

        assertThat(result).isEqualTo("success");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void retriesTooManyRequests() {
        GeminiRetryExecutor executor = new GeminiRetryExecutor(3, 0);
        AtomicInteger attempts = new AtomicInteger();
        RestClientResponseException tooManyRequests = responseException(HttpStatus.TOO_MANY_REQUESTS);

        String result = executor.execute(() -> {
            if (attempts.incrementAndGet() == 1) throw tooManyRequests;
            return "success";
        });

        assertThat(result).isEqualTo("success");
        assertThat(attempts).hasValue(2);
    }

    @Test
    void doesNotRetryNonRetryableResponse() {
        GeminiRetryExecutor executor = new GeminiRetryExecutor(3, 0);
        AtomicInteger attempts = new AtomicInteger();
        RestClientResponseException badRequest = responseException(HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> executor.execute(() -> {
            attempts.incrementAndGet();
            throw badRequest;
        })).isSameAs(badRequest);
        assertThat(attempts).hasValue(1);
    }

    @Test
    void stopsAfterConfiguredAttemptLimit() {
        GeminiRetryExecutor executor = new GeminiRetryExecutor(3, 0);
        AtomicInteger attempts = new AtomicInteger();
        RestClientResponseException unavailable = responseException(HttpStatus.SERVICE_UNAVAILABLE);

        assertThatThrownBy(() -> executor.execute(() -> {
            attempts.incrementAndGet();
            throw unavailable;
        })).isSameAs(unavailable);
        assertThat(attempts).hasValue(3);
    }

    private RestClientResponseException responseException(HttpStatus status) {
        RestClientResponseException exception = mock(RestClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(status);
        return exception;
    }
}
