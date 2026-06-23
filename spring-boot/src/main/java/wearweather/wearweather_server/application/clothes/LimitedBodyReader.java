package wearweather.wearweather_server.application.clothes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class LimitedBodyReader {
    private LimitedBodyReader() {
    }

    static byte[] read(InputStream inputStream, int maxBytes) throws IOException {
        try (inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new BodyTooLargeException();
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    static final class BodyTooLargeException extends IOException {
    }
}
