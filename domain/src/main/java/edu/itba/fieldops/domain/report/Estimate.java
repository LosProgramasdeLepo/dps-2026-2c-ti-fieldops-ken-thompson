package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record Estimate(Duration duration, RiskLevel risk, Map<ConsumableId, Stock> estimatedConsumption) {
    public Estimate {
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(risk, "risk");
        estimatedConsumption = Map.copyOf(estimatedConsumption);
    }
}
