package wearweather.wearweather_server.application.clothes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.clothes.dto.ClothesResponse;
import wearweather.wearweather_server.domain.clothes.repository.UserClothesJpaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClothesQueryService {
    private final UserClothesJpaRepository userClothesRepository;

    @Transactional(readOnly = true)
    public List<ClothesResponse> getMine(AuthenticatedUser user) {
        return userClothesRepository.findActiveItemsByUserId(user.id()).stream()
                .map(item -> ClothesResponse.from(item.getClothes(), item.getFavorite()))
                .toList();
    }
}
