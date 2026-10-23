package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Consumption;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ActivityExecution {
    private final ActivityId activityId;
    private final Instant startedAt;
    private final Optional<Completion> completion;

    public ActivityExecution(ActivityId activityId, Instant startedAt) {
        this.activityId = Objects.requireNonNull(activityId, "activity id");
        this.startedAt = Objects.requireNonNull(startedAt, "started at");
        this.completion = Optional.empty();
    }

    private ActivityExecution(ActivityId activityId, Instant startedAt, Completion completion) {
        this.activityId = activityId;
        this.startedAt = startedAt;
        this.completion = Optional.of(completion);
    }

    public ActivityExecution finish(Instant finishedAt, ActivityReport report) {
        if (completion.isPresent()) {
            throw new InvalidActivityExecution("activity already finished: " + activityId);
        }
        Objects.requireNonNull(finishedAt, "finished at");
        if (finishedAt.isBefore(startedAt)) {
            throw new InvalidActivityExecution("finish must not be before start");
        }
        return new ActivityExecution(activityId, startedAt, new Completion(finishedAt, report));
    }

    public ActivityId activityId() {
        return activityId;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Optional<Instant> finishedAt() {
        return completion.map(Completion::finishedAt);
    }

    public Optional<Outcome> result() {
        return completion.map(finished -> finished.report().result());
    }

    public Map<ConsumableId, Consumption> consumed() {
        return completion.map(finished -> finished.report().consumed()).orElse(Map.of());
    }

    public boolean isFinished() {
        return completion.isPresent();
    }

    private record Completion(Instant finishedAt, ActivityReport report) {
        private Completion {
            Objects.requireNonNull(finishedAt, "finished at");
            Objects.requireNonNull(report, "activity report");
        }
    }
}
