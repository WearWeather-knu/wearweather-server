package wearweather.wearweather_server.application.user.dto;

public record UpdateUserCommand(
        String nickname,
        Integer age
) {
}
