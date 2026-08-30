package com.lapwise.lapwise_backend.adapter.out.persistence.activityinsight;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataActivityInsightRepository extends JpaRepository<ActivityInsightEntity, UUID> {
    Optional<ActivityInsightEntity> findByActivity_Id(UUID activityId);
}
