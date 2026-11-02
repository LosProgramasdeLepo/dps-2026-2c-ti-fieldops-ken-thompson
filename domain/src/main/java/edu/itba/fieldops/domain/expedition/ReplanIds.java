package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.ProposalId;

import java.util.Objects;

public record ReplanIds(ProposalId proposal, ExpeditionId revision) {
    public ReplanIds {
        Objects.requireNonNull(proposal, "proposal id");
        Objects.requireNonNull(revision, "revision id");
    }
}
