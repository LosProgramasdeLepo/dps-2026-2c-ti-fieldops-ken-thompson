package edu.itba.fieldops.adapters.jpa.expedition;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Embeddable
public record StoredActivity(
        @Column(name = "activity_id", nullable = false) UUID activityId,
        @Column(name = "name", nullable = false) String name,
        @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
        @Column(name = "estimated_duration", nullable = false) Duration estimatedDuration,
        @Column(name = "risk", nullable = false) String risk,
        @Column(name = "zone", nullable = false) String zone,
        @Column(name = "window_start", nullable = false) Instant windowStart,
        @Column(name = "window_end", nullable = false) Instant windowEnd,
        @Column(name = "required_vehicles", nullable = false) int requiredVehicles
) {
}
