package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Observation;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record OperationalReport(
        OperationalStatus status,
        int plannedActivities,
        int startedActivities,
        int finishedActivities,
        Duration duration,
        RiskLevel risk,
        Map<ConsumableId, Consumption> consumption,
        Map<ConsumableId, Consumption> estimatedConsumption,
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
        boolean started = !execution.activities().isEmpty();
        return new OperationalReport(
                OperationalStatus.of(execution),
                expedition.activities().size(),
                execution.activities().size(),
                finished.size(),
                started ? actualDuration(expedition, finished) : estimate.duration(),
                estimate.risk(),
                consumed(finished),
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

    private static Map<ConsumableId, Consumption> consumed(List<ActivityExecution> finished) {
        Map<ConsumableId, Consumption> totals = new HashMap<>();
        for (ActivityExecution run : finished) {
            run.consumed().forEach((id, amount) -> totals.merge(id, amount, Consumption::plus));
        }
        return totals;
    }

    private static List<ActivityResult> activityResults(List<ActivityExecution> finished) {
        return finished.stream()
                .map(run -> new ActivityResult(run.activityId(), run.result().orElseThrow()))
                .toList();
    }
}
