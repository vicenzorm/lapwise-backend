package com.lapwise.lapwise_backend.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.lapwise.lapwise_backend.domain.model.User;
import com.lapwise.lapwise_backend.domain.port.out.UserRepositoryPort;

@Repository
@Transactional
public class UserPersistenceAdapter implements UserRepositoryPort {
    private final SpringDataUserRepository userRepository;

    public UserPersistenceAdapter(SpringDataUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserMapper.toEntity(user);
        UserEntity savedEntity = userRepository.save(entity);
        return UserMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByStravaAthleteId(Long id) {
        return userRepository.findByStravaAthleteId(id).map(UserMapper::toDomain);
    }

    @Override
    public void deleteUserById(UUID id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id).map(UserMapper::toDomain);
    }
}
