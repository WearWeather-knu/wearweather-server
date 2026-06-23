package wearweather.wearweather_server.application.user.dto;

import wearweather.wearweather_server.domain.user.Gender;
import wearweather.wearweather_server.domain.user.StylePreference;
import wearweather.wearweather_server.domain.user.User;

import java.util.UUID;

public record UserResult(
        UUID id,
        String email,
        String nickname,
        Integer age,
        Gender gender,
        Float sensitivityOffset,
        StylePreference stylePreference
) {

    public static UserResult from(User user) {
        return new UserResult(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAge(),
                user.getGender(),
                user.getSensitivityOffset(),
                user.getStylePreference()
        );
    }
}
