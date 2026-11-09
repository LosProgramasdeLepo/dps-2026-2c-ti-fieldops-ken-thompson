package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.IncidentResponder;
import edu.itba.fieldops.domain.expedition.ReplanIds;
import edu.itba.fieldops.domain.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.expedition.Runs;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.usecase.expedition.RespondToIncident;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class RespondToIncidentInteractor implements RespondToIncident {
    private final ExpeditionRepository plans;
    private final Runs runs;
    private final IncidentResponder responder;
    private final ReplanProposalRepository proposals;
    private final EventLog events;

    public RespondToIncidentInteractor(
            ExpeditionRepository plans,
            Runs runs,
            IncidentResponder responder,
            ReplanProposalRepository proposals,
            EventLog events
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.runs = Objects.requireNonNull(runs, "runs");
        this.responder = Objects.requireNonNull(responder, "incident responder");
        this.proposals = Objects.requireNonNull(proposals, "proposals");
        this.events = Objects.requireNonNull(events, "event log");
    }

    @Override
    public void respondTo(ExpeditionId runId, IncidentId incidentId) {
        ExpeditionExecution run = runs.find(runId)
                .orElseThrow(() -> new UnknownResource("run", runId.value().toString()));
        responder.respondTo(run, incidentId, new ReplanIds(proposals.nextId(), plans.nextId())).ifPresent(proposal -> {
            proposals.save(proposal);
            events.append(proposal.pullEvents());
        });
    }
}
