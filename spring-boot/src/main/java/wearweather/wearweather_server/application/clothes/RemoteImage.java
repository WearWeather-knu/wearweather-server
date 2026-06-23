package wearweather.wearweather_server.application.clothes;

public record RemoteImage(
        byte[] bytes,
        String contentType,
        String extension
) {
}
