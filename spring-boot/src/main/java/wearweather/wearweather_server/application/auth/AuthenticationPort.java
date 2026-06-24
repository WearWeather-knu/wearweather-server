package wearweather.wearweather_server.application.auth;

public interface AuthenticationPort {
    AuthenticatedUser authenticate(String authorizationHeader);
}
