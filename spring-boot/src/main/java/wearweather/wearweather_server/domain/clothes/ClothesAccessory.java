package wearweather.wearweather_server.domain.clothes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clothes_acc")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesAccessory {
    @Id @Column(name = "clothes_id") private Long clothesId;
    @MapsId @OneToOne @JoinColumn(name = "clothes_id") private Clothes clothes;
    @Column(nullable = false, length = 30) private String type;
    @Column(name = "warmth_bonus") private Integer warmthBonus;
    @Column(length = 30) private String color;

    public ClothesAccessory(Clothes clothes, String type, Integer warmthBonus, String color) {
        this.clothes = clothes;
        this.type = type;
        this.warmthBonus = warmthBonus;
        this.color = color;
    }
}
