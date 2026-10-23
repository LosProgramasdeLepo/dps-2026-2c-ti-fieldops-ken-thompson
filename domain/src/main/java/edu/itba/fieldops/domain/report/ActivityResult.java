package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.tracking.Outcome;

import java.util.Objects;

public record ActivityResult(ActivityId activityId, Outcome result) {
    public ActivityResult {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(result, "result");
    }
}
