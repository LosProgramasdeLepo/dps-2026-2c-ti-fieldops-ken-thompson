package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionTracker;
import edu.itba.fieldops.domain.expedition.IncidentResponder;
import edu.itba.fieldops.domain.expedition.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.expedition.ReplanIds;
import edu.itba.fieldops.domain.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.IncidentReport;
import edu.itba.fieldops.domain.usecase.expedition.RecordIncident;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class RecordIncidentInteractor implements RecordIncident {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final ExpeditionTracker tracker;
    private final IncidentResponder responder;
    private final ReplanProposalRepository proposals;
    private final EventLog events;

    public RecordIncidentInteractor(
            ExpeditionRepository plans,
            ExecutionRepository executions,
            ExpeditionTracker tracker,
            IncidentResponder responder,
            ReplanProposalRepository proposals,
            EventLog events
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.executions = Objects.requireNonNull(executions, "executions");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.responder = Objects.requireNonNull(responder, "incident responder");
        this.proposals = Objects.requireNonNull(proposals, "proposals");
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
        Incident incident = tracker.reportIncident(execution, IncidentReport.affecting(incidentId, activityId, description));
        executions.save(execution);
        events.append(execution.pullEvents());
        responder.respondTo(execution, incident, new ReplanIds(proposals.nextId(), plans.nextId())).ifPresent(proposal -> {
            proposals.save(proposal);
            events.append(proposal.pullEvents());
        });
        return incidentId;
    }

    private ExpeditionExecution requireRun(ExpeditionId expeditionId) {
        return executions.find(expeditionId)
                .orElseThrow(() -> new InvalidExpeditionTransition(plans.require(expeditionId).status(), "record incident"));
    }
}
