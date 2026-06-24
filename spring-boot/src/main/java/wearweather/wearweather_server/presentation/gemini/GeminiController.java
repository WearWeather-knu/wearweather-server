package wearweather.wearweather_server.presentation.gemini;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.auth.SupabaseAuthService;
import wearweather.wearweather_server.application.gemini.GeminiService;
import wearweather.wearweather_server.application.gemini.OutfitRecommendationService;
import wearweather.wearweather_server.application.user.UserService;
import wearweather.wearweather_server.presentation.gemini.dto.GeminiGenerateRequest;
import wearweather.wearweather_server.presentation.gemini.dto.GeminiGenerateResponse;
import wearweather.wearweather_server.presentation.gemini.dto.OutfitImageRecommendationRequest;
import wearweather.wearweather_server.presentation.gemini.dto.OutfitImageRecommendationResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gemini")
public class GeminiController {

    private final SupabaseAuthService supabaseAuthService;
    private final GeminiService geminiService;
    private final OutfitRecommendationService outfitRecommendationService;
    private final UserService userService;

    @PostMapping("/generate")
    public GeminiGenerateResponse generate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody GeminiGenerateRequest request
    ) {
        supabaseAuthService.authenticate(authorizationHeader);
        return new GeminiGenerateResponse(geminiService.generate(request.prompt()));
    }

    @PostMapping("/outfit-images")
    public OutfitImageRecommendationResponse recommendOutfitImages(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody OutfitImageRecommendationRequest request
    ) {
        AuthenticatedUser authenticatedUser = supabaseAuthService.authenticate(authorizationHeader);
        return outfitRecommendationService.recommend(
                userService.getMe(authenticatedUser),
                request
        );
    }
}
