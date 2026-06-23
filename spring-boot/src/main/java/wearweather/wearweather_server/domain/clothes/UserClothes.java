package wearweather.wearweather_server.domain.clothes;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_clothes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserClothes {
    @EmbeddedId
    private UserClothesId id;

    public UserClothes(UUID userId, Long clothesId) {
        this.id = new UserClothesId(userId, clothesId);
    }
}
