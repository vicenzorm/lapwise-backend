package com.lapwise.lapwise_backend.domain.port.in;

import com.lapwise.lapwise_backend.domain.model.User;
import com.lapwise.lapwise_backend.domain.port.in.command.CompleteStravaAuthCommand;

public interface CompleteStravaAuthUseCase {
    User complete(CompleteStravaAuthCommand command);
}
