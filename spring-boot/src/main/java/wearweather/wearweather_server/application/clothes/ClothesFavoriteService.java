package wearweather.wearweather_server.application.clothes;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.clothes.dto.ClothesFavoriteResponse;
import wearweather.wearweather_server.domain.clothes.entity.UserClothes;
import wearweather.wearweather_server.domain.clothes.entity.UserClothesId;
import wearweather.wearweather_server.domain.clothes.repository.UserClothesJpaRepository;

@Service
@RequiredArgsConstructor
public class ClothesFavoriteService {
    private final UserClothesJpaRepository userClothesRepository;

    @Transactional
    public ClothesFavoriteResponse setFavorite(AuthenticatedUser user, Long clothesId, boolean favorite) {
        UserClothesId id = new UserClothesId(user.id(), clothesId);
        UserClothes userClothes = userClothesRepository.findById(id)
                .orElseThrow(() -> new ClothesException(
                        HttpStatus.NOT_FOUND,
                        "USER_CLOTHES_NOT_FOUND",
                        "사용자의 옷장에서 해당 의류를 찾을 수 없습니다."
                ));
        userClothes.updateFavorite(favorite);
        return new ClothesFavoriteResponse(clothesId, favorite);
    }
}
