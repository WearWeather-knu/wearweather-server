package wearweather.wearweather_server.domain.clothes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import wearweather.wearweather_server.domain.clothes.type.ClothesCategory;

@Entity
@Getter
@Table(name = "clothes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clothes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClothesCategory category;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "original_url")
    private String originalUrl;

    @Column(name = "min_temp", nullable = false)
    private Float minTemp;

    @Column(name = "max_temp", nullable = false)
    private Float maxTemp;

    @Column(name = "is_active")
    private Boolean active = true;

    public Clothes(String name, ClothesCategory category, String imageUrl, String originalUrl,
                   Float minTemp, Float maxTemp) {
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
        this.originalUrl = originalUrl;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.active = true;
    }
}
