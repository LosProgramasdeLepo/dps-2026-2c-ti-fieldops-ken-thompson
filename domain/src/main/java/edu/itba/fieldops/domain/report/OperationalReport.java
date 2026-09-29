package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.Incident;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record OperationalReport(
        ExpeditionStatus status,
        int plannedActivities,
        int startedActivities,
        int finishedActivities,
        Duration duration,
        RiskLevel risk,
        Map<ConsumableId, Stock> consumption,
        Map<ConsumableId, Stock> estimatedConsumption,
        List<Incident> incidents,
        List<ActivityResult> activityResults
) {
    public OperationalReport {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(risk, "risk");
        consumption = Map.copyOf(consumption);
        estimatedConsumption = Map.copyOf(estimatedConsumption);
        incidents = List.copyOf(incidents);
        activityResults = List.copyOf(activityResults);
    }

    public static OperationalReport of(Expedition expedition) {
        return of(expedition, null);
    }

    public static OperationalReport of(Expedition expedition, ExpeditionExecution execution) {
        Objects.requireNonNull(expedition, "expedition");
        List<ActivityExecution> executions = execution == null ? List.of() : execution.executions();
        return new OperationalReport(
                expedition.status(),
                expedition.itinerary().size(),
                executions.size(),
                (int) executions.stream().filter(ActivityExecution::isFinished).count(),
                totalDuration(expedition),
                highestRisk(expedition),
                expedition.assignments().consumption(),
                estimatedConsumption(expedition),
                execution == null ? List.of() : execution.incidents(),
                activityResults(executions)
        );
    }

    private static Duration totalDuration(Expedition expedition) {
        return expedition.itinerary().stream()
                .map(Activity::estimatedDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    private static RiskLevel highestRisk(Expedition expedition) {
        return expedition.itinerary().stream()
                .map(Activity::risk)
                .max(Comparator.naturalOrder())
                .orElse(RiskLevel.LOW);
    }

    private static Map<ConsumableId, Stock> estimatedConsumption(Expedition expedition) {
        Map<ConsumableId, Stock> totals = new HashMap<>();
        for (Activity activity : expedition.itinerary()) {
            activity.requirements().estimatedConsumption()
                    .forEach((id, quantity) -> totals.merge(id, quantity, Stock::plus));
        }
        return totals;
    }

    private static List<ActivityResult> activityResults(List<ActivityExecution> executions) {
        List<ActivityResult> results = new ArrayList<>();
        for (ActivityExecution execution : executions) {
            execution.result().ifPresent(result -> results.add(new ActivityResult(execution.activityId(), result)));
        }
        return results;
    }
}
