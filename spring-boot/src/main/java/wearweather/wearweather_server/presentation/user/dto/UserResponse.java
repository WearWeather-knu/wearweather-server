package wearweather.wearweather_server.presentation.user.dto;

import wearweather.wearweather_server.application.user.dto.UserResult;
import wearweather.wearweather_server.domain.user.Gender;
import wearweather.wearweather_server.domain.user.StylePreference;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String nickname,
        Integer age,
        Gender gender,
        Float sensitivityOffset,
        StylePreference stylePreference
) {

    public static UserResponse from(UserResult user) {
        return new UserResponse(
                user.id(),
                user.email(),
                user.nickname(),
                user.age(),
                user.gender(),
                user.sensitivityOffset(),
                user.stylePreference()
        );
    }
}
