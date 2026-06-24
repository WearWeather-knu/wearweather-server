package wearweather.wearweather_server.domain.clothes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record UserClothesId(
        @Column(name = "user_id") UUID userId,
        @Column(name = "clothes_id") Long clothesId
) implements Serializable {
}
