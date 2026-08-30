package com.lapwise.lapwise_backend.domain.port.in;

import java.util.Optional;

import com.lapwise.lapwise_backend.domain.model.SwimActivity;
import com.lapwise.lapwise_backend.domain.port.in.command.GetSwimActivityCommand;

public interface GetSwimActivityUseCase {
    Optional<SwimActivity> getSwimActivity(GetSwimActivityCommand command);
}
