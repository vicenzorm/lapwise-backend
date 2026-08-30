package com.lapwise.lapwise_backend.domain.usecase;

import java.util.Optional;

import com.lapwise.lapwise_backend.domain.model.StravaTokenSet;
import com.lapwise.lapwise_backend.domain.model.User;
import com.lapwise.lapwise_backend.domain.port.in.CompleteStravaAuthUseCase;
import com.lapwise.lapwise_backend.domain.port.in.DeleteUserUseCase;
import com.lapwise.lapwise_backend.domain.port.in.command.CompleteStravaAuthCommand;
import com.lapwise.lapwise_backend.domain.port.in.command.DeleteUserCommand;
import com.lapwise.lapwise_backend.domain.port.out.StravaAuthPort;
import com.lapwise.lapwise_backend.domain.port.out.UserRepositoryPort;

public class UserService implements DeleteUserUseCase, CompleteStravaAuthUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final StravaAuthPort stravaAuthPort;

    public UserService(UserRepositoryPort userRepositoryPort, StravaAuthPort stravaAuthPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.stravaAuthPort = stravaAuthPort;
    }

    @Override
    public void deleteUser(DeleteUserCommand command) {
        userRepositoryPort.deleteUserById(command.id());
    }

    @Override
    public User complete(CompleteStravaAuthCommand command) {
        StravaTokenSet tokenSet = stravaAuthPort.exchangeAuthorizationCode(command.code());
        Optional<User> user = userRepositoryPort.findByStravaAthleteId(tokenSet.stravaAthleteId());
        if (user.isEmpty()) {
            User newUser = User.createNew(
                null,
                tokenSet.stravaAthleteId(),
                tokenSet.accessToken(),
                tokenSet.refreshToken(),
                tokenSet.tokenExpiresAt()
            );
            return userRepositoryPort.save(newUser);
        }
        User existingUser = user.get();
        User updatedUser = new User(
            existingUser.id(),
            existingUser.email(),
            existingUser.stravaAthleteId(),
            tokenSet.accessToken(),
            tokenSet.refreshToken(),
            tokenSet.tokenExpiresAt(),
            existingUser.lastSyncedAt()
        );
        return userRepositoryPort.save(updatedUser);
    }
}
