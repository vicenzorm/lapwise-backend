package com.lapwise.lapwise_backend.domain.port.in;

import com.lapwise.lapwise_backend.domain.model.SwimActivityPage;
import com.lapwise.lapwise_backend.domain.port.in.command.GetSwimActivitiesCommand;

public interface GetSwimActivitiesUseCase {
    SwimActivityPage getSwimActivities(GetSwimActivitiesCommand command);
}
