package wearweather.wearweather_server.domain.clothes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClothesJpaRepository extends JpaRepository<Clothes, Long> {
    Optional<Clothes> findByOriginalUrl(String originalUrl);
}
