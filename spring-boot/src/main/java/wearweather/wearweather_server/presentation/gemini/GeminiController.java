package wearweather.wearweather_server.presentation.gemini;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wearweather.wearweather_server.application.auth.SupabaseAuthService;
import wearweather.wearweather_server.application.gemini.GeminiService;
import wearweather.wearweather_server.presentation.gemini.dto.GeminiGenerateRequest;
import wearweather.wearweather_server.presentation.gemini.dto.GeminiGenerateResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gemini")
public class GeminiController {

    private final SupabaseAuthService supabaseAuthService;
    private final GeminiService geminiService;

    @PostMapping("/generate")
    public GeminiGenerateResponse generate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody GeminiGenerateRequest request
    ) {
        supabaseAuthService.authenticate(authorizationHeader);
        return new GeminiGenerateResponse(geminiService.generate(request.prompt()));
    }
}
