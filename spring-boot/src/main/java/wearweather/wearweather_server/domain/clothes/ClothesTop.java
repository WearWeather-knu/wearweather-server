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
@Table(name = "clothes_tops")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesTop {
    @Id
    @Column(name = "clothes_id")
    private Long clothesId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "clothes_id")
    private Clothes clothes;

    @Enumerated(EnumType.STRING)
    @Column(name = "sleeve_length", length = 20)
    private SleeveLength sleeveLength;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Thickness thickness;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ClothesFit fit;

    @Column(length = 50)
    private String material;

    @Column(length = 30)
    private String color;

    public ClothesTop(Clothes clothes, SleeveLength sleeveLength, Thickness thickness,
                      ClothesFit fit, String material, String color) {
        this.clothes = clothes;
        this.sleeveLength = sleeveLength;
        this.thickness = thickness;
        this.fit = fit;
        this.material = material;
        this.color = color;
    }
}
