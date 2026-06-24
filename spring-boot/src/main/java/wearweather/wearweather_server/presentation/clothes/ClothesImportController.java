package wearweather.wearweather_server.presentation.clothes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.auth.SupabaseAuthService;
import wearweather.wearweather_server.application.clothes.ClothesImportService;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportPreviewRequest;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportPreviewResponse;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportRequest;
import wearweather.wearweather_server.application.clothes.dto.ClothesImportResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clothes/import")
public class ClothesImportController {
    private final SupabaseAuthService authService;
    private final ClothesImportService importService;

    @PostMapping("/preview")
    public ClothesImportPreviewResponse preview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody ClothesImportPreviewRequest request
    ) {
        AuthenticatedUser user = authService.authenticate(authorizationHeader);
        return importService.preview(user, request);
    }

    @PostMapping
    public ResponseEntity<ClothesImportResponse> save(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody ClothesImportRequest request
    ) {
        AuthenticatedUser user = authService.authenticate(authorizationHeader);
        ClothesImportResponse response = importService.save(user, request);
        return ResponseEntity.status(response.closetLinked() ? HttpStatus.CREATED : HttpStatus.OK).body(response);
    }
}
