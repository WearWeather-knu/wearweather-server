package wearweather.wearweather_server.domain.clothes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wearweather.wearweather_server.domain.clothes.entity.ClothesAccessory;

public interface ClothesAccessoryJpaRepository extends JpaRepository<ClothesAccessory, Long> {
}
