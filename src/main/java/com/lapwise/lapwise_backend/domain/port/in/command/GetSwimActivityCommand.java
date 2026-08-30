package com.lapwise.lapwise_backend.domain.port.in.command;

import java.util.UUID;

public record GetSwimActivityCommand(
    UUID id,
    UUID userId
) {}
