package wearweather.wearweather_server.domain.recommendation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "recommendation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "weather_id")
    private Long weatherId;
    @Column(name = "ai_suggestion", nullable = false)
    private String aiSuggestion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> topIds;
    @Column(name = "bottom_id")
    private Long bottomId;
    @Column(name = "outer_id")
    private Long outerId;
    @Column(name = "shoes_id")
    private Long shoesId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "acc_ids", columnDefinition = "jsonb")
    private List<Long> accIds;
    @Column(name = "bag_id")
    private Long bagId;
    @Column(name = "image_url", nullable = false)
    private String imageUrl;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "is_like")
    private Boolean liked;

    public Recommendation(UUID userId, Long weatherId, String aiSuggestion,
                          List<Long> topIds, Long bottomId, Long outerId, Long shoesId,
                          List<Long> accIds, Long bagId, String imageUrl) {
        this.userId = userId;
        this.weatherId = weatherId;
        this.aiSuggestion = aiSuggestion;
        this.topIds = List.copyOf(topIds);
        this.bottomId = bottomId;
        this.outerId = outerId;
        this.shoesId = shoesId;
        this.accIds = List.copyOf(accIds);
        this.bagId = bagId;
        this.imageUrl = imageUrl;
        this.createdAt = Instant.now();
        this.liked = false;
    }
}
