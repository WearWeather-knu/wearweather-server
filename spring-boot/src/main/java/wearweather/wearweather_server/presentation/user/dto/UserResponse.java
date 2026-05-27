package wearweather.wearweather_server.presentation.user.dto;

import wearweather.wearweather_server.application.user.dto.UserResult;
import wearweather.wearweather_server.domain.user.Gender;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String nickname,
        Integer age,
        Gender gender
) {

    public static UserResponse from(UserResult user) {
        return new UserResponse(
                user.id(),
                user.email(),
                user.nickname(),
                user.age(),
                user.gender()
        );
    }
}
