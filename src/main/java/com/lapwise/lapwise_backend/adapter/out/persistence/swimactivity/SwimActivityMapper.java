package com.lapwise.lapwise_backend.adapter.out.persistence.swimactivity;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lapwise.lapwise_backend.adapter.out.persistence.user.UserEntity;
import com.lapwise.lapwise_backend.domain.model.Split;
import com.lapwise.lapwise_backend.domain.model.SwimActivity;

public class SwimActivityMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
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
            readSplits(swimActivityEntity.getRawSplitsJson())
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
            writeSplits(swimActivity.splits())
        );
    }

    private static String writeSplits(List<Split> splits) {
        if (splits == null || splits.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(splits);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write splits JSON", exception);
        }
    }
    
    private static List<Split> readSplits(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Split>>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read splits JSON", exception);
        }
    }
}
