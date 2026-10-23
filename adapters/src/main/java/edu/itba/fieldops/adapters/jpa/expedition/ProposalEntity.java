package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.adapters.jpa.tracking.StoredIncident;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ProposalState;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "replan_proposals")
public class ProposalEntity {
    @Id
    private UUID id;

    @Column(name = "original_id", nullable = false)
    private UUID originalId;

    @Column(name = "suggested_id", nullable = false)
    private UUID suggestedId;

    @Embedded
    @AttributeOverride(name = "description", column = @Column(name = "incident_description", nullable = false))
    @AttributeOverride(name = "occurredAt", column = @Column(name = "incident_at", nullable = false))
    @AttributeOverride(name = "activityId", column = @Column(name = "incident_activity_id", nullable = false))
    private StoredIncident incident;

    @Column(nullable = false)
    private String decision;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "registration_order", insertable = false, updatable = false)
    private Long registrationOrder;

    protected ProposalEntity() {
    }

    ProposalEntity(ProposalState state) {
        this.id = state.id().value();
        this.originalId = state.originalId().value();
        this.suggestedId = state.suggestedId().value();
        this.incident = StoredIncident.of(state.incident());
        this.decision = state.decision().name();
        this.decidedBy = state.decidedBy().map(PersonId::value).orElse(null);
        this.decidedAt = state.decidedAt().orElse(null);
    }

    UUID suggestedId() {
        return suggestedId;
    }

    ReplanProposal toDomain(Expedition suggested) {
        ReplanProposal.Decision recorded = ReplanProposal.Decision.valueOf(decision);
        Optional<PersonId> by = decidedBy == null ? Optional.empty() : Optional.of(new PersonId(decidedBy));
        return ReplanProposal.restore(new ProposalState(
                new ProposalId(id),
                new ExpeditionId(originalId),
                new ExpeditionId(suggestedId),
                incident.toDomain(),
                recorded,
                by,
                Optional.ofNullable(decidedAt)
        ), suggested);
    }
}
