package wearweather.wearweather_server.application.auth;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email
) {
}
