package wearweather.wearweather_server.domain.clothes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "clothes_bags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesBag {
    @Id @Column(name = "clothes_id") private Long clothesId;
    @MapsId @OneToOne @JoinColumn(name = "clothes_id") private Clothes clothes;
    @Column(length = 30) private String type;
    @Column(length = 50) private String material;
    @Column(length = 30) private String color;
    @Column(name = "is_waterproof") private Boolean waterproof;

    public ClothesBag(Clothes clothes, String type, String material, String color, Boolean waterproof) {
        this.clothes = clothes;
        this.type = type;
        this.material = material;
        this.color = color;
        this.waterproof = waterproof;
    }
}
