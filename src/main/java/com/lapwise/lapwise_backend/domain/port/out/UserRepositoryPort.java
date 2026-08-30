package com.lapwise.lapwise_backend.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.lapwise.lapwise_backend.domain.model.User;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByStravaAthleteId(Long id);
    Optional<User> findById(UUID id);
    void deleteUserById(UUID id);
}
