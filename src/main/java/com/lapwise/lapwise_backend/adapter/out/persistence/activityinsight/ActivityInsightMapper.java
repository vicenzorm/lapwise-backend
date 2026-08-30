package com.lapwise.lapwise_backend.adapter.out.persistence.activityinsight;

import com.lapwise.lapwise_backend.adapter.out.persistence.swimactivity.SwimActivityEntity;
import com.lapwise.lapwise_backend.domain.model.ActivityInsight;

public class ActivityInsightMapper {
    public static ActivityInsight toDomain(ActivityInsightEntity activityInsightEntity) {
        if (activityInsightEntity == null) {
            return null;
        }
        return new ActivityInsight(
            activityInsightEntity.getId(),
            activityInsightEntity.getActivity().getId(),
            activityInsightEntity.getBody(),
            activityInsightEntity.getCreatedAt()
        );
    }

    public static ActivityInsightEntity toEntity(ActivityInsight activityInsight, SwimActivityEntity swimActivityEntity) {
        if (activityInsight == null) {
            return null;
        }

        return new ActivityInsightEntity(
            activityInsight.id(),
            swimActivityEntity, 
            activityInsight.body(),
            activityInsight.createdAt()
        );
    }
}
