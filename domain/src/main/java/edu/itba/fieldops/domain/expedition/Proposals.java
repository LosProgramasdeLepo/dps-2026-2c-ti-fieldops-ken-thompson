package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.IncidentId;

import java.util.Optional;

public interface Proposals {
    Optional<ReplanProposal> forIncident(IncidentId id);
}
