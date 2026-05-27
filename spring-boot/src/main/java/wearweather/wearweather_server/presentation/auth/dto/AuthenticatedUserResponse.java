package wearweather.wearweather_server.presentation.auth.dto;

import wearweather.wearweather_server.application.auth.AuthenticatedUser;

import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID id,
        String email
) {

    public static AuthenticatedUserResponse from(AuthenticatedUser authenticatedUser) {
        return new AuthenticatedUserResponse(
                authenticatedUser.id(),
                authenticatedUser.email()
        );
    }
}
