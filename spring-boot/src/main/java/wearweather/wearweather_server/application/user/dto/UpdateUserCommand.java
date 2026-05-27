package wearweather.wearweather_server.application.user.dto;

import wearweather.wearweather_server.domain.user.Gender;

public record UpdateUserCommand(
        String nickname,
        Integer age
) {
}
