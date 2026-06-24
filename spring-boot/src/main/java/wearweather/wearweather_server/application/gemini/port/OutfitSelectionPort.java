package wearweather.wearweather_server.application.gemini.port;

import wearweather.wearweather_server.application.gemini.OutfitCandidate;
import wearweather.wearweather_server.application.gemini.OutfitSelection;
import wearweather.wearweather_server.application.user.dto.UserResult;
import wearweather.wearweather_server.domain.weather.WeatherLog;

import java.util.List;

public interface OutfitSelectionPort {
    OutfitSelection select(WeatherLog weather, UserResult user, String requestedStyle,
                           List<OutfitCandidate> candidates);
}
