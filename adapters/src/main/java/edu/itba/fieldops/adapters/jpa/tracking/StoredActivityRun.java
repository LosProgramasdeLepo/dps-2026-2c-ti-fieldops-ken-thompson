package edu.itba.fieldops.adapters.jpa.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.ActivityReport;
import edu.itba.fieldops.domain.tracking.Outcome;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
                execution.result().map(Outcome::text).orElse(null)
        );
    }

    ActivityExecution toDomain(Collection<StoredActivityConsumption> consumption) {
        ActivityExecution started = new ActivityExecution(new ActivityId(activityId), startedAt);
        Map<ConsumableId, Consumption> consumed = consumption.stream()
                .filter(row -> row.activityId().equals(activityId))
                .collect(Collectors.toMap(row -> new ConsumableId(row.consumableId()), row -> new Consumption(row.quantity())));
        return Optional.ofNullable(finishedAt)
                .map(finished -> started.finish(finished, new ActivityReport(new Outcome(result), consumed)))
                .orElse(started);
    }
}
