package com.lapwise.lapwise_backend.adapter.out.persistence;

import com.lapwise.lapwise_backend.domain.model.User;

public class UserMapper {
    public static User toDomain(UserEntity userEntity) {
        if (userEntity == null) {
            return null;
        }
        return new User(
            userEntity.getId(),
            userEntity.getEmail(),
            userEntity.getStravaAthleteId(),
            userEntity.getAccessToken(),
            userEntity.getRefreshToken(),
            userEntity.getTokenExpiresAt(),
            userEntity.getLastSyncedAt()
        );
    }

    public static UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserEntity(
            user.id(),
            user.email(),
            user.stravaAthleteId(),
            user.accessToken(),
            user.refreshToken(),
            user.tokenExpiresAt(),
            user.lastSyncedAt()
        );
    }
}
