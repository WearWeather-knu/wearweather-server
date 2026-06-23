package wearweather.wearweather_server.application.user.dto;

import wearweather.wearweather_server.domain.user.Gender;
import wearweather.wearweather_server.domain.user.StylePreference;

public record UpdateUserCommand(
        String nickname,
        Integer age,
        Gender gender,
        Float sensitivityOffset,
        StylePreference stylePreference
) {
}
