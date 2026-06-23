package wearweather.wearweather_server.application.user;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.user.dto.UpdateUserCommand;
import wearweather.wearweather_server.application.user.dto.UserResult;
import wearweather.wearweather_server.domain.user.User;
import wearweather.wearweather_server.domain.user.UserJpaRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserJpaRepository userJpaRepository;

    @Transactional
    public UserResult getMe(AuthenticatedUser authenticatedUser) {
        return UserResult.from(findOrCreateUser(authenticatedUser));
    }

    @Transactional
    public UserResult updateMe(AuthenticatedUser authenticatedUser, UpdateUserCommand command) {
        User user = findOrCreateUser(authenticatedUser);
        user.updateProfile(command.nickname(), command.age(), command.gender(), command.sensitivityOffset(), command.stylePreference());
        return UserResult.from(user);
    }

    private User findOrCreateUser(AuthenticatedUser authenticatedUser) {
        return userJpaRepository.findById(authenticatedUser.id())
                .orElseGet(() -> {
                    try {
                        return userJpaRepository.saveAndFlush(new User(
                                authenticatedUser.id(),
                                authenticatedUser.email()
                        ));
                    } catch (DataIntegrityViolationException e) {
                        return userJpaRepository.findById(authenticatedUser.id())
                                .orElseThrow(() -> e);
                    }
                });
    }
}
