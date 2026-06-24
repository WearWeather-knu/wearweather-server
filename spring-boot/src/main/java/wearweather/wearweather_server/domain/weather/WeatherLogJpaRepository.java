package wearweather.wearweather_server.domain.weather;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherLogJpaRepository extends JpaRepository<WeatherLog, Long> {
}
