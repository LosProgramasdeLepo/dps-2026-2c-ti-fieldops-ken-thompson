package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Observation;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record OperationalReport(
        OperationalStatus status,
        int plannedActivities,
        int startedActivities,
        int finishedActivities,
        Duration duration,
        RiskLevel risk,
        Map<ConsumableId, Stock> consumption,
        Map<ConsumableId, Stock> estimatedConsumption,
        List<Incident> incidents,
        List<Observation> observations,
        List<ActivityResult> activityResults
) {
    public OperationalReport {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(risk, "risk");
        consumption = Map.copyOf(consumption);
        estimatedConsumption = Map.copyOf(estimatedConsumption);
        incidents = List.copyOf(incidents);
        observations = List.copyOf(observations);
        activityResults = List.copyOf(activityResults);
    }

    public static OperationalReport of(Expedition expedition) {
        Estimate estimate = Estimate.of(expedition);
        return new OperationalReport(
                OperationalStatus.of(expedition),
                expedition.activities().size(),
                0,
                0,
                estimate.duration(),
                estimate.risk(),
                expedition.assignments().consumption(),
                estimate.estimatedConsumption(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    public static OperationalReport of(Expedition expedition, ExpeditionExecution execution) {
        Objects.requireNonNull(execution, "execution");
        Estimate estimate = Estimate.of(expedition);
        List<ActivityExecution> finished = execution.activities().stream().filter(ActivityExecution::isFinished).toList();
        return new OperationalReport(
                OperationalStatus.of(execution),
                expedition.activities().size(),
                execution.activities().size(),
                finished.size(),
                actualDuration(expedition, finished),
                estimate.risk(),
                consumed(expedition, finished),
                estimate.estimatedConsumption(),
                execution.incidents(),
                execution.observations(),
                activityResults(finished)
        );
    }

    private static Duration actualDuration(Expedition expedition, List<ActivityExecution> finished) {
        Map<ActivityId, Duration> measured = new HashMap<>();
        for (ActivityExecution run : finished) {
            measured.put(run.activityId(), Duration.between(run.startedAt(), run.finishedAt().orElseThrow()));
        }
        return expedition.duration(activity -> measured.getOrDefault(activity.id(), Duration.ZERO));
    }

    private static Map<ConsumableId, Stock> consumed(Expedition expedition, List<ActivityExecution> finished) {
        Set<ActivityId> finishedIds = finished.stream().map(ActivityExecution::activityId).collect(Collectors.toSet());
        Map<ConsumableId, Stock> totals = new HashMap<>();
        for (ConsumableAssignment assignment : expedition.assignments().consumables()) {
            if (finishedIds.contains(assignment.activityId())) {
                totals.merge(assignment.consumableId(), assignment.quantity(), Stock::plus);
            }
        }
        return totals;
    }

    private static List<ActivityResult> activityResults(List<ActivityExecution> finished) {
        return finished.stream()
                .map(run -> new ActivityResult(run.activityId(), run.result().orElseThrow()))
                .toList();
    }
}
