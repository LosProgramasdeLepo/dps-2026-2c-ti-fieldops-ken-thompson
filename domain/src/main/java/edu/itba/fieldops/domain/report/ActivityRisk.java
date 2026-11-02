package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.shared.RiskLevel;

import java.util.Objects;

public record ActivityRisk(ActivityId activityId, RiskLevel risk) {
    public ActivityRisk {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(risk, "risk");
    }
}
