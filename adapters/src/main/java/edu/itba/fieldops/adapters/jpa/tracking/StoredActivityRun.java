package edu.itba.fieldops.adapters.jpa.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Embeddable
public record StoredActivityRun(
        @Column(name = "activity_id", nullable = false) UUID activityId,
        @Column(name = "started_at", nullable = false) Instant startedAt,
        @Column(name = "finished_at") Instant finishedAt,
        @Column(name = "result") String result
) {
    static StoredActivityRun of(ActivityExecution execution) {
        return new StoredActivityRun(
                execution.activityId().value(),
                execution.startedAt(),
                execution.finishedAt().orElse(null),
                execution.result().orElse(null)
        );
    }

    ActivityExecution toDomain() {
        ActivityExecution started = new ActivityExecution(new ActivityId(activityId), startedAt);
        return Optional.ofNullable(finishedAt)
                .map(finished -> started.finish(finished, result))
                .orElse(started);
    }
}
