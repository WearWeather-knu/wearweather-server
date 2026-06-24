package wearweather.wearweather_server.domain.clothes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wearweather.wearweather_server.domain.clothes.entity.UserClothes;
import wearweather.wearweather_server.domain.clothes.entity.UserClothesId;

public interface UserClothesJpaRepository extends JpaRepository<UserClothes, UserClothesId> {
}
