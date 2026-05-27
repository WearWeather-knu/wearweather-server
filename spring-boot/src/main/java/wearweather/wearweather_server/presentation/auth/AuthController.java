package wearweather.wearweather_server.presentation.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.auth.SupabaseAuthService;
import wearweather.wearweather_server.presentation.auth.dto.AuthenticatedUserResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final SupabaseAuthService supabaseAuthService;

    @GetMapping("/me")
    public AuthenticatedUserResponse getMe(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        AuthenticatedUser authenticatedUser = supabaseAuthService.authenticate(authorizationHeader);
        return AuthenticatedUserResponse.from(authenticatedUser);
    }
}
