package wearweather.wearweather_server.domain.clothes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wearweather.wearweather_server.domain.clothes.entity.Clothes;
import wearweather.wearweather_server.domain.clothes.entity.UserClothes;
import wearweather.wearweather_server.domain.clothes.entity.UserClothesId;

import java.util.List;
import java.util.UUID;

public interface UserClothesJpaRepository extends JpaRepository<UserClothes, UserClothesId> {
    @Query("""
            select c as clothes, uc.favorite as favorite
            from UserClothes uc
            join Clothes c on c.id = uc.id.clothesId
            where uc.id.userId = :userId and c.active = true
            order by c.category, c.id
            """)
    List<UserClothesItem> findActiveItemsByUserId(@Param("userId") UUID userId);

    interface UserClothesItem {
        Clothes getClothes();

        boolean getFavorite();
    }
}
