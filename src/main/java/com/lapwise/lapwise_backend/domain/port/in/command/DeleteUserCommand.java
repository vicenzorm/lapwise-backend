package com.lapwise.lapwise_backend.domain.port.in.command;

import java.util.UUID;

public record DeleteUserCommand(
    UUID id
) { }
