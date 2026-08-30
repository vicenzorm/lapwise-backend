package com.lapwise.lapwise_backend.adapter.out.persistence.swimactivity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.lapwise.lapwise_backend.adapter.out.persistence.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "swim_activities",
    uniqueConstraints = @UniqueConstraint(
        columnNames = { "user_id", "strava_activity_id" }
    )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SwimActivityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "swim_activity_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "strava_activity_id", nullable = false)
    private long stravaActivityId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "distance_meters", nullable = false)
    private double distanceMeters;

    @Column(name = "pool_length_meters")
    private Double poolLengthMeters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_splits_json", columnDefinition = "jsonb")
    private String rawSplitsJson;
}
