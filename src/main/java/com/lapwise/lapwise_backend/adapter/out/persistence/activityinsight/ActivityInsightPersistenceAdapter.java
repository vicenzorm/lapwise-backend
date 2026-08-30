package com.lapwise.lapwise_backend.adapter.out.persistence.activityinsight;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.lapwise.lapwise_backend.adapter.out.persistence.swimactivity.SpringDataSwimActivityRepository;
import com.lapwise.lapwise_backend.adapter.out.persistence.swimactivity.SwimActivityEntity;
import com.lapwise.lapwise_backend.domain.model.ActivityInsight;
import com.lapwise.lapwise_backend.domain.port.out.ActivityInsightRepositoryPort;

@Repository
@Transactional
public class ActivityInsightPersistenceAdapter implements ActivityInsightRepositoryPort {

    private final SpringDataActivityInsightRepository activityInsightRepository;
    private final SpringDataSwimActivityRepository swimActivityRepository;

    public ActivityInsightPersistenceAdapter(SpringDataActivityInsightRepository activityInsightRepository, SpringDataSwimActivityRepository swimActivityRepository) {
        this.activityInsightRepository = activityInsightRepository;
        this.swimActivityRepository = swimActivityRepository;
    }

    @Override
    public ActivityInsight save(ActivityInsight activityInsight) {
        SwimActivityEntity activity = swimActivityRepository.getReferenceById(activityInsight.activityId());
        ActivityInsightEntity entity = ActivityInsightMapper.toEntity(activityInsight, activity);
        ActivityInsightEntity savedEntity = activityInsightRepository.save(entity);
        return ActivityInsightMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ActivityInsight> findActivityInsightBySwimActivityId(UUID swimActivityId) {
        return activityInsightRepository
            .findByActivity_Id(swimActivityId)
            .map(ActivityInsightMapper::toDomain);
    }
}
