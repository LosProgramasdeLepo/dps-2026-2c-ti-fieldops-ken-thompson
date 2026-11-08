package edu.itba.fieldops.adapters.mongo.expedition;

import edu.itba.fieldops.adapters.mongo.tracking.IncidentDocument;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ProposalState;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Document(collection = ProposalDocument.COLLECTION)
record ProposalDocument(
        @Id String id,
        String originalId,
        String suggestedId,
        IncidentDocument incident,
        String decision,
        String decidedBy,
        Instant decidedAt,
        long registrationOrder
) {
    static final String COLLECTION = "replan_proposals";

    static ProposalDocument of(ProposalState state, long registrationOrder) {
        return new ProposalDocument(
                state.id().value().toString(),
                state.originalId().value().toString(),
                state.suggestedId().value().toString(),
                IncidentDocument.of(state.incident()),
                state.decision().name(),
                state.decidedBy().map(id -> id.value().toString()).orElse(null),
                state.decidedAt().orElse(null),
                registrationOrder
        );
    }

    ReplanProposal toDomain(Expedition suggested) {
        return ReplanProposal.restore(new ProposalState(
                new ProposalId(UUID.fromString(id)),
                new ExpeditionId(UUID.fromString(originalId)),
                new ExpeditionId(UUID.fromString(suggestedId)),
                incident.toDomain(id + "#incident"),
                ReplanProposal.Decision.valueOf(decision),
                Optional.ofNullable(decidedBy).map(UUID::fromString).map(PersonId::new),
                Optional.ofNullable(decidedAt)
        ), suggested);
    }
}
