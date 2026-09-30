package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.Incident;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class ReplanProposal {
    public enum Decision {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    private final ProposalId id;
    private final ExpeditionId originalId;
    private final Incident incident;
    private final Expedition suggested;
    private Decision decision;
    private PersonId decidedBy;
    private Instant decidedAt;

    ReplanProposal(ProposalId id, ExpeditionId originalId, Incident incident, Expedition suggested) {
        this.id = Objects.requireNonNull(id, "proposal id");
        this.originalId = Objects.requireNonNull(originalId, "original expedition id");
        this.incident = Objects.requireNonNull(incident, "incident");
        this.suggested = Objects.requireNonNull(suggested, "suggested plan");
        this.decision = Decision.PENDING;
        if (incident.activityId() == null) {
            throw new InvalidValue("incident must affect an activity");
        }
    }

    void accept(PersonId responsible, Instant at) {
        decide(Decision.ACCEPTED, responsible, at);
    }

    void reject(PersonId responsible, Instant at) {
        decide(Decision.REJECTED, responsible, at);
    }

    public ProposalId id() {
        return id;
    }

    public ExpeditionId originalId() {
        return originalId;
    }

    public Incident incident() {
        return incident;
    }

    public Expedition suggested() {
        return suggested;
    }

    public Decision decision() {
        return decision;
    }

    public Optional<PersonId> decidedBy() {
        return Optional.ofNullable(decidedBy);
    }

    public Optional<Instant> decidedAt() {
        return Optional.ofNullable(decidedAt);
    }

    private void decide(Decision next, PersonId responsible, Instant at) {
        if (decision != Decision.PENDING) {
            throw new InvalidValue("proposal already decided");
        }
        this.decidedBy = Objects.requireNonNull(responsible, "responsible");
        this.decidedAt = Objects.requireNonNull(at, "decision time");
        this.decision = next;
    }
}
