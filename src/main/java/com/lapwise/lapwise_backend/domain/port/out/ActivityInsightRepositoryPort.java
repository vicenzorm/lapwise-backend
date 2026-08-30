package com.lapwise.lapwise_backend.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.lapwise.lapwise_backend.domain.model.ActivityInsight;

public interface ActivityInsightRepositoryPort {
    ActivityInsight save(ActivityInsight activityInsight);
    Optional<ActivityInsight> findActivityInsightBySwimActivityId(UUID swimActivityId);
}
