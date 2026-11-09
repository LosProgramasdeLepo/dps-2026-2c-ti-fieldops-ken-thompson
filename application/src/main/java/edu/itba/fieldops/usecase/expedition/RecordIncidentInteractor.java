package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.event.Deliveries;
import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionTracker;
import edu.itba.fieldops.domain.expedition.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.expedition.Proposals;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.IncidentReport;
import edu.itba.fieldops.domain.usecase.expedition.IncidentSnapshot;
import edu.itba.fieldops.domain.usecase.expedition.RecordIncident;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class RecordIncidentInteractor implements RecordIncident {
    private static final String REPORTED = IncidentReported.class.getSimpleName();

    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final ExpeditionTracker tracker;
    private final Proposals proposals;
    private final Deliveries deliveries;
    private final EventLog events;

    public RecordIncidentInteractor(
            ExpeditionRepository plans,
            ExecutionRepository executions,
            ExpeditionTracker tracker,
            Proposals proposals,
            Deliveries deliveries,
            EventLog events
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.executions = Objects.requireNonNull(executions, "executions");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.proposals = Objects.requireNonNull(proposals, "proposals");
        this.deliveries = Objects.requireNonNull(deliveries, "deliveries");
        this.events = Objects.requireNonNull(events, "event log");
    }

    @Override
    public IncidentId record(ExpeditionId expeditionId, IncidentDescription description) {
        ExpeditionExecution execution = requireRun(expeditionId);
        IncidentId incidentId = executions.nextIncidentId();
        tracker.reportIncident(execution, IncidentReport.general(incidentId, description));
        executions.save(execution);
        events.append(execution.pullEvents());
        return incidentId;
    }

    @Override
    public IncidentId record(ExpeditionId expeditionId, IncidentDescription description, ActivityId activityId) {
        ExpeditionExecution execution = requireRun(expeditionId);
        IncidentId incidentId = executions.nextIncidentId();
        tracker.reportIncident(execution, IncidentReport.affecting(incidentId, activityId, description));
        executions.save(execution);
        events.append(execution.pullEvents());
        return incidentId;
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentSnapshot incident(ExpeditionId runId, IncidentId incidentId) {
        Incident incident = executions.find(runId)
                .flatMap(run -> run.incidents().stream().filter(candidate -> candidate.id().equals(incidentId)).findFirst())
                .orElseThrow(() -> new UnknownResource("incident", incidentId.value().toString()));
        return new IncidentSnapshot(
                incident,
                proposals.forIncident(incidentId).map(ReplanProposal::id),
                processingOf(runId, incidentId)
        );
    }

    private List<Delivery> processingOf(ExpeditionId runId, IncidentId incidentId) {
        return reportOf(runId, incidentId).map(deliveries::of).orElse(List.of());
    }

    private Optional<EventId> reportOf(ExpeditionId runId, IncidentId incidentId) {
        EventFilter reported = EventFilter.forExpeditions(Set.of(runId)).withTypes(Set.of(REPORTED));
        Page<RecordedEvent> page = events.page(reported, new PageRequest(0, PageRequest.MAX_SIZE));
        for (int number = 1; ; number++) {
            Optional<EventId> found = page.items().stream()
                    .filter(item -> item.event() instanceof IncidentReported report && report.incidentId().equals(incidentId))
                    .map(RecordedEvent::id)
                    .findFirst();
            if (found.isPresent() || number >= page.totalPages()) {
                return found;
            }
            page = events.page(reported, new PageRequest(number, PageRequest.MAX_SIZE));
        }
    }

    private ExpeditionExecution requireRun(ExpeditionId expeditionId) {
        return executions.find(expeditionId)
                .orElseThrow(() -> new InvalidExpeditionTransition(plans.require(expeditionId).status(), "record incident"));
    }
}
