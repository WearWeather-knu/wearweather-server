package wearweather.wearweather_server.presentation.clothes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.auth.AuthenticationPort;
import wearweather.wearweather_server.application.clothes.ClothesFavoriteService;
import wearweather.wearweather_server.application.clothes.ClothesQueryService;
import wearweather.wearweather_server.application.clothes.dto.ClothesFavoriteRequest;
import wearweather.wearweather_server.application.clothes.dto.ClothesFavoriteResponse;
import wearweather.wearweather_server.application.clothes.dto.ClothesResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clothes")
public class ClothesController {
    private final AuthenticationPort authenticationPort;
    private final ClothesQueryService clothesQueryService;
    private final ClothesFavoriteService clothesFavoriteService;

    @GetMapping
    public List<ClothesResponse> getMine(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        AuthenticatedUser user = authenticationPort.authenticate(authorizationHeader);
        return clothesQueryService.getMine(user);
    }

    @PatchMapping("/{clothesId}/favorite")
    public ClothesFavoriteResponse setFavorite(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long clothesId,
            @Valid @RequestBody ClothesFavoriteRequest request
    ) {
        AuthenticatedUser user = authenticationPort.authenticate(authorizationHeader);
        return clothesFavoriteService.setFavorite(user, clothesId, request.favorite());
    }
}
