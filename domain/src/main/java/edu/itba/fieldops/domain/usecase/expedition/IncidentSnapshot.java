package edu.itba.fieldops.domain.usecase.expedition;

import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.tracking.Incident;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record IncidentSnapshot(Incident incident, Optional<ProposalId> proposal, List<Delivery> processing) {
    public IncidentSnapshot {
        Objects.requireNonNull(incident, "incident");
        Objects.requireNonNull(proposal, "proposal");
        processing = List.copyOf(processing);
    }
}
