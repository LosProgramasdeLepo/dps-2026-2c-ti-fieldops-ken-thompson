package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.shared.Texts;

import java.util.Objects;

public record ActivityResult(ActivityId activityId, String result) {
    public ActivityResult {
        Objects.requireNonNull(activityId, "activity id");
        result = Texts.required(result, "result");
    }
}
