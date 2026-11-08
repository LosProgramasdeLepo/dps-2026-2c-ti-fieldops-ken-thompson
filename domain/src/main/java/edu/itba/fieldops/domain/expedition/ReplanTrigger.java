package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.tracking.Incident;

import java.time.Instant;
import java.util.Objects;

record ReplanTrigger(ProposalId id, Incident incident, Instant at) {
    ReplanTrigger {
        Objects.requireNonNull(id, "proposal id");
        Objects.requireNonNull(incident, "incident");
        Objects.requireNonNull(at, "proposal time");
    }
}
