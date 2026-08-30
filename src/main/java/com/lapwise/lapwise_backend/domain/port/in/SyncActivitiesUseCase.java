package com.lapwise.lapwise_backend.domain.port.in;

import com.lapwise.lapwise_backend.domain.model.SyncResult;
import com.lapwise.lapwise_backend.domain.port.in.command.SyncActivitiesCommand;

public interface SyncActivitiesUseCase {
    SyncResult sync(SyncActivitiesCommand command);
}
