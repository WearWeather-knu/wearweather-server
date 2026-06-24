package wearweather.wearweather_server.application.clothes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.clothes.dto.ClothesResponse;
import wearweather.wearweather_server.domain.clothes.ClothesJpaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClothesQueryService {
    private final ClothesJpaRepository clothesRepository;

    @Transactional(readOnly = true)
    public List<ClothesResponse> getMine(AuthenticatedUser user) {
        return clothesRepository.findActiveByUserId(user.id()).stream()
                .map(ClothesResponse::from)
                .toList();
    }
}
