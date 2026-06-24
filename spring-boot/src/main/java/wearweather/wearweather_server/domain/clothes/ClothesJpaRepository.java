package wearweather.wearweather_server.domain.clothes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClothesJpaRepository extends JpaRepository<Clothes, Long> {
    Optional<Clothes> findByOriginalUrl(String originalUrl);

    @Query("""
            select c from Clothes c
            join UserClothes uc on uc.id.clothesId = c.id
            where uc.id.userId = :userId and c.active = true
            order by c.category, c.id
            """)
    List<Clothes> findActiveByUserId(@Param("userId") UUID userId);
}
