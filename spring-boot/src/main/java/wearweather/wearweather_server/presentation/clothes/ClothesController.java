package wearweather.wearweather_server.presentation.clothes;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.auth.AuthenticationPort;
import wearweather.wearweather_server.application.clothes.ClothesQueryService;
import wearweather.wearweather_server.application.clothes.dto.ClothesResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clothes")
public class ClothesController {
    private final AuthenticationPort authenticationPort;
    private final ClothesQueryService clothesQueryService;

    @GetMapping
    public List<ClothesResponse> getMine(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        AuthenticatedUser user = authenticationPort.authenticate(authorizationHeader);
        return clothesQueryService.getMine(user);
    }
}
