package wearweather.wearweather_server.presentation.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.auth.SupabaseAuthService;
import wearweather.wearweather_server.application.user.UserService;
import wearweather.wearweather_server.presentation.user.dto.UpdateUserRequest;
import wearweather.wearweather_server.presentation.user.dto.UserResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final SupabaseAuthService supabaseAuthService;
    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getMe(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        AuthenticatedUser authenticatedUser = supabaseAuthService.authenticate(authorizationHeader);
        return UserResponse.from(userService.getMe(authenticatedUser));
    }

    @PutMapping("/me")
    public UserResponse updateMe(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody UpdateUserRequest request
    ) {
        AuthenticatedUser authenticatedUser = supabaseAuthService.authenticate(authorizationHeader);
        return UserResponse.from(userService.updateMe(authenticatedUser, request.toCommand()));
    }
}
