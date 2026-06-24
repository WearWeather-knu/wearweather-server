package wearweather.wearweather_server.domain.clothes.entity;

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
import wearweather.wearweather_server.domain.clothes.type.BottomLength;
import wearweather.wearweather_server.domain.clothes.type.ClothesFit;

@Entity
@Getter
@Table(name = "clothes_bottoms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesBottom {
    @Id @Column(name = "clothes_id") private Long clothesId;
    @MapsId @OneToOne @JoinColumn(name = "clothes_id") private Clothes clothes;
    @Enumerated(EnumType.STRING) @Column(length = 20) private BottomLength length;
    @Enumerated(EnumType.STRING) @Column(length = 20) private ClothesFit fit;
    @Column(length = 50) private String material;
    @Column(length = 30) private String color;

    public ClothesBottom(Clothes clothes, BottomLength length, ClothesFit fit, String material, String color) {
        this.clothes = clothes;
        this.length = length;
        this.fit = fit;
        this.material = material;
        this.color = color;
    }
}
