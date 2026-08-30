package com.lapwise.lapwise_backend.domain.port.in;

import com.lapwise.lapwise_backend.domain.port.in.command.DeleteUserCommand;

public interface DeleteUserUseCase {
    void deleteUser(DeleteUserCommand command);
}
