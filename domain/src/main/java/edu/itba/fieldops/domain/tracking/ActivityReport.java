package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Consumption;

import java.util.Map;
import java.util.Objects;

public record ActivityReport(Outcome result, Map<ConsumableId, Consumption> consumed) {
    public ActivityReport {
        Objects.requireNonNull(result, "result");
        consumed = Map.copyOf(consumed);
    }

    public static ActivityReport of(Outcome result) {
        return new ActivityReport(result, Map.of());
    }
}
