package com.lapwise.lapwise_backend.adapter.out.persistence.swimactivity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.lapwise.lapwise_backend.adapter.out.persistence.user.SpringDataUserRepository;
import com.lapwise.lapwise_backend.adapter.out.persistence.user.UserEntity;
import com.lapwise.lapwise_backend.domain.model.SwimActivity;
import com.lapwise.lapwise_backend.domain.port.out.SwimActivityRepositoryPort;

@Repository
@Transactional
public class SwimActivityPersistenceAdapter implements SwimActivityRepositoryPort {

    private final SpringDataSwimActivityRepository swimActivityRepository;
    private final SpringDataUserRepository userRepository;

    public SwimActivityPersistenceAdapter(
        SpringDataSwimActivityRepository swimActivityRepository,
        SpringDataUserRepository userRepository
    ) {
        this.swimActivityRepository = swimActivityRepository;
        this.userRepository = userRepository;
    }

    @Override
    public SwimActivity save(SwimActivity swimActivity) {
        UserEntity user = userRepository.getReferenceById(swimActivity.userId());
        SwimActivityEntity entity = SwimActivityMapper.toEntity(swimActivity, user);
        SwimActivityEntity saved = swimActivityRepository.save(entity);
        return SwimActivityMapper.toDomain(saved);
    }

    @Override
    public boolean existsByUserIdAndStravaActivityId(UUID userId, Long stravaActivityId) {
        return swimActivityRepository.existsByUser_IdAndStravaActivityId(userId, stravaActivityId);
    }

    @Override
    public List<SwimActivity> findPage(UUID userId, Instant cursorStartedAt, UUID cursorId, int limit) {
        var pageable = cursorStartedAt == null
            ? PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startedAt").and(Sort.by(Sort.Direction.DESC, "id")))
            : PageRequest.of(0, limit);
        List<SwimActivityEntity> rows = cursorStartedAt == null
            ? swimActivityRepository.findByUser_Id(userId, pageable)
            : swimActivityRepository.findAfterCursor(userId, cursorStartedAt, cursorId, pageable);
        return rows.stream().map(SwimActivityMapper::toDomain).toList();
    }

    @Override
    public Optional<SwimActivity> findSwimActivityByIdAndUserId(UUID id, UUID userId) {
        return swimActivityRepository.findByIdAndUser_Id(id, userId).map(SwimActivityMapper::toDomain);
    }
}
