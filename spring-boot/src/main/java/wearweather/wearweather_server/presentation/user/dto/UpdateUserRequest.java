package wearweather.wearweather_server.presentation.user.dto;

import wearweather.wearweather_server.application.user.dto.UpdateUserCommand;
import wearweather.wearweather_server.domain.user.Gender;
import wearweather.wearweather_server.domain.user.StylePreference;

public record UpdateUserRequest(
        String nickname,
        Integer age,
        Gender gender,
        Float sensitivityOffset,
        StylePreference stylePreference
) {

    public UpdateUserCommand toCommand() {
        return new UpdateUserCommand(nickname, age, gender, sensitivityOffset, stylePreference);
    }
}
