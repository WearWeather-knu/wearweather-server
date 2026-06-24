package wearweather.wearweather_server.domain.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationJpaRepository extends JpaRepository<Recommendation, Long> {
}
