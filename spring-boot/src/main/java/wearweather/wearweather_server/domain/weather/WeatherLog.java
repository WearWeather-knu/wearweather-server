package wearweather.wearweather_server.domain.weather;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Table(name = "weather_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_name", nullable = false)
    private String locationName;
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;
    @Column(name = "current_temp", nullable = false)
    private Float currentTemp;
    @Column(name = "temp_min")
    private Float tempMin;
    @Column(name = "temp_max")
    private Float tempMax;
    @Column(name = "feels_like")
    private Float feelsLike;
    private Float precipitation;
    @Column(name = "uv_index")
    private Float uvIndex;
    private Integer humidity;
    @Column(name = "wind_speed")
    private Float windSpeed;
    @Column(name = "sky_status")
    private String skyStatus;
    @Column(name = "fetched_at")
    private Instant fetchedAt;
    private Integer pop;
    private Integer pm10;
    private Integer pm25;
}
