package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.Clock;

import java.util.Objects;

public final class ProposalReviewer {
    private final Plans plans;
    private final Clock clock;

    public ProposalReviewer(Plans plans, Clock clock) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void accept(ReplanProposal proposal, PersonId responsible) {
        Expedition original = originalOf(proposal);
        proposal.accept(original, responsible, clock.now());
    }

    public void reject(ReplanProposal proposal, PersonId responsible) {
        Expedition original = originalOf(proposal);
        proposal.reject(original, responsible, clock.now());
    }

    private Expedition originalOf(ReplanProposal proposal) {
        return plans.require(proposal.originalId());
    }
}
