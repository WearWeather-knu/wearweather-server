package wearweather.wearweather_server.domain.clothes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "clothes_outers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesOuter {
    @Id @Column(name = "clothes_id") private Long clothesId;
    @MapsId @OneToOne @JoinColumn(name = "clothes_id") private Clothes clothes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Thickness thickness;
    @Enumerated(EnumType.STRING) @Column(length = 20) private ClothesFit fit;
    @Column(name = "is_windproof") private Boolean windproof;
    @Column(name = "is_waterproof") private Boolean waterproof;
    @Column(nullable = false, length = 50) private String material;
    @Column(nullable = false, length = 30) private String color;

    public ClothesOuter(Clothes clothes, Thickness thickness, ClothesFit fit, Boolean windproof,
                        Boolean waterproof, String material, String color) {
        this.clothes = clothes;
        this.thickness = thickness;
        this.fit = fit;
        this.windproof = windproof;
        this.waterproof = waterproof;
        this.material = material;
        this.color = color;
    }
}
