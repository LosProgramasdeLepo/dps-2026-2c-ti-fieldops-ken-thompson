package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record Estimate(Duration duration, RiskLevel risk, Map<ConsumableId, Stock> estimatedConsumption) {
    public Estimate {
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(risk, "risk");
        estimatedConsumption = Map.copyOf(estimatedConsumption);
    }

    public static Estimate of(Expedition expedition) {
        Objects.requireNonNull(expedition, "expedition");
        Map<ConsumableId, Stock> estimated = new HashMap<>();
        for (Activity activity : expedition.itinerary()) {
            activity.requirements().estimatedConsumption()
                    .forEach((id, quantity) -> estimated.merge(id, quantity, Stock::plus));
        }
        return new Estimate(
                expedition.itinerary().stream()
                        .map(Activity::estimatedDuration)
                        .reduce(Duration.ZERO, Duration::plus),
                expedition.itinerary().stream()
                        .map(Activity::risk)
                        .max(Comparator.naturalOrder())
                        .orElse(RiskLevel.LOW),
                estimated
        );
    }
}
