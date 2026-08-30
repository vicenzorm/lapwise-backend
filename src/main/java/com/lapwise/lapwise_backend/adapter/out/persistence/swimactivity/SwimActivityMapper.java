package com.lapwise.lapwise_backend.adapter.out.persistence.swimactivity;

import com.lapwise.lapwise_backend.adapter.out.persistence.user.UserEntity;
import com.lapwise.lapwise_backend.domain.model.SwimActivity;

public class SwimActivityMapper {
    public static SwimActivity toDomain(SwimActivityEntity swimActivityEntity) {
        if (swimActivityEntity == null) {
            return null;
        }
        return new SwimActivity(
            swimActivityEntity.getId(),
            swimActivityEntity.getUser().getId(),
            swimActivityEntity.getStravaActivityId(),
            swimActivityEntity.getStartedAt(),
            swimActivityEntity.getDurationSeconds(),
            swimActivityEntity.getDistanceMeters(),
            swimActivityEntity.getPoolLengthMeters(),
            swimActivityEntity.getRawSplitsJson()
        );
    }

    public static SwimActivityEntity toEntity(SwimActivity swimActivity, UserEntity user) {
        if (swimActivity == null) {
            return null;
        }
        return new SwimActivityEntity(
            swimActivity.id(),
            user,
            swimActivity.stravaActivityId(),
            swimActivity.startedAt(),
            swimActivity.durationSeconds(),
            swimActivity.distanceMeters(),
            swimActivity.poolLengthMeters(),
            swimActivity.rawSplitsJson()
        );
    }
}
