package edu.itba.fieldops.adapters.mongo.tracking;

import edu.itba.fieldops.domain.expedition.ExecutionState;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.ActivityReport;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.domain.tracking.ObservationText;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Document(collection = "runs")
record RunDocument(
        @Id String expeditionId,
        String status,
        List<ActivityRun> activities,
        List<IncidentDocument> incidents,
        List<ObservationEntry> observations
) {
    static RunDocument of(ExecutionState state) {
        return new RunDocument(
                state.expeditionId().value().toString(),
                state.status().name(),
                state.activities().stream().map(ActivityRun::of).toList(),
                state.incidents().stream().map(IncidentDocument::of).toList(),
                state.observations().stream().map(ObservationEntry::of).toList()
        );
    }

    ExpeditionExecution toDomain() {
        return ExpeditionExecution.restore(new ExecutionState(
                new ExpeditionId(UUID.fromString(expeditionId)),
                ExpeditionExecution.Status.valueOf(status),
                activities.stream().map(ActivityRun::toDomain).toList(),
                incidents.stream().map(IncidentDocument::toDomain).toList(),
                observations.stream().map(ObservationEntry::toDomain).toList()
        ));
    }

    record ActivityRun(String activityId, Instant startedAt, Instant finishedAt, String result, Map<String, Integer> consumed) {
        static ActivityRun of(ActivityExecution execution) {
            return new ActivityRun(
                    execution.activityId().value().toString(),
                    execution.startedAt(),
                    execution.finishedAt().orElse(null),
                    execution.result().map(Outcome::text).orElse(null),
                    execution.consumed().entrySet().stream().collect(Collectors.toMap(
                            entry -> entry.getKey().value().toString(),
                            entry -> entry.getValue().amount()
                    ))
            );
        }

        ActivityExecution toDomain() {
            ActivityExecution started = new ActivityExecution(new ActivityId(UUID.fromString(activityId)), startedAt);
            if (finishedAt == null) {
                return started;
            }
            Map<ConsumableId, Consumption> amounts = consumed.entrySet().stream().collect(Collectors.toMap(
                    entry -> new ConsumableId(UUID.fromString(entry.getKey())),
                    entry -> new Consumption(entry.getValue())
            ));
            return started.finish(finishedAt, new ActivityReport(new Outcome(result), amounts));
        }
    }

    record ObservationEntry(String text, Instant at) {
        static ObservationEntry of(Observation observation) {
            return new ObservationEntry(observation.text().text(), observation.at());
        }

        Observation toDomain() {
            return new Observation(new ObservationText(text), at);
        }
    }
}
