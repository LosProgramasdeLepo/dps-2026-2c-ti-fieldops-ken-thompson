package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionResponses.ProposalResponse;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.usecase.expedition.ProposalSnapshot;

final class ReplanMapping {
    private ReplanMapping() {
    }

    static ProposalResponse proposal(ProposalSnapshot snapshot) {
        return new ProposalResponse(
                snapshot.id().value(),
                snapshot.originalId().value(),
                RunMapping.incident(snapshot.incident()),
                snapshot.decision().name(),
                snapshot.decidedBy().map(PersonId::value).orElse(null),
                snapshot.decidedAt().orElse(null),
                PlanMapping.expedition(snapshot.suggested())
        );
    }
}
