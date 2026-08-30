package com.lapwise.lapwise_backend.adapter.out.persistence.swimactivity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSwimActivityRepository extends JpaRepository<SwimActivityEntity, UUID> {
    boolean existsByUser_IdAndStravaActivityId(UUID userId, Long stravaActivityId);

    List<SwimActivityEntity> findByUser_Id(UUID userId, Pageable pageable);

    @Query("""
        select s from SwimActivityEntity s
        where s.user.id = :userId
          and (s.startedAt < :cursorStartedAt
               or (s.startedAt = :cursorStartedAt and s.id < :cursorId))
        order by s.startedAt desc, s.id desc
        """)
    List<SwimActivityEntity> findAfterCursor(
        @Param("userId") UUID userId,
        @Param("cursorStartedAt") Instant cursorStartedAt,
        @Param("cursorId") UUID cursorId,
        Pageable pageable
    );

    Optional<SwimActivityEntity> findByIdAndUser_Id(UUID id, UUID userId);
}
