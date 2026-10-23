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
    private final Incident incident;
    private final Expedition suggested;
    private final ExpeditionId originalId;
    private Decision decision;
    private PersonId decidedBy;
    private Instant decidedAt;

    public ReplanProposal(ProposalId id, Incident incident, Expedition suggested) {
        this.id = Objects.requireNonNull(id, "proposal id");
        this.incident = Objects.requireNonNull(incident, "incident");
        this.suggested = Objects.requireNonNull(suggested, "suggested plan");
        this.originalId = suggested.supersedes()
                .orElseThrow(() -> new InvalidValue("suggested plan must revise an approved plan"));
        if (incident.activityId().isEmpty()) {
            throw new InvalidValue("incident must affect an activity");
        }
        this.decision = Decision.PENDING;
    }

    public static ReplanProposal restore(ProposalState state, Expedition suggested) {
        Objects.requireNonNull(state, "proposal state");
        if (!Objects.requireNonNull(suggested, "suggested plan").id().equals(state.suggestedId())) {
            throw new InvalidValue("suggested plan does not match the proposal");
        }
        ReplanProposal proposal = new ReplanProposal(state.id(), state.incident(), suggested);
        if (!proposal.originalId.equals(state.originalId())) {
            throw new InvalidValue("suggested plan does not revise the recorded original");
        }
        if (state.decision() != Decision.PENDING) {
            proposal.decidedBy = state.decidedBy().orElseThrow();
            proposal.decidedAt = state.decidedAt().orElseThrow();
            proposal.decision = state.decision();
        }
        return proposal;
    }

    public ProposalState state() {
        return new ProposalState(id, originalId, suggested.id(), incident, decision, decidedBy(), decidedAt());
    }

    public void accept(Expedition original, Responsible responsible, Instant at) {
        requireResponsible(original, responsible);
        if (original.status() != ExpeditionStatus.APPROVED) {
            throw new InvalidExpeditionTransition(original.status(), "accept replan");
        }
        decide(Decision.ACCEPTED, responsible.personId(), at);
    }

    public void reject(Expedition original, Responsible responsible, Instant at) {
        requireResponsible(original, responsible);
        decide(Decision.REJECTED, responsible.personId(), at);
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

    private static void requireResponsible(Expedition original, Responsible responsible) {
        if (!Objects.requireNonNull(responsible, "responsible").belongsTo(Objects.requireNonNull(original, "original"))) {
            throw new InvalidValue("replan must be decided by a responsible");
        }
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
