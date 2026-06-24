package wearweather.wearweather_server.domain.clothes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.util.UUID;

@Entity
@Table(name = "user_clothes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserClothes {
    @EmbeddedId
    private UserClothesId id;

    @Column(name = "is_favorite", nullable = false)
    @ColumnDefault("false")
    private boolean favorite;

    public UserClothes(UUID userId, Long clothesId) {
        this.id = new UserClothesId(userId, clothesId);
    }

    public void updateFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
