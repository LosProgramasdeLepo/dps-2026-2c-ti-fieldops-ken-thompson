package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.expedition.usecase.ProposalSnapshot;
import edu.itba.fieldops.domain.expedition.usecase.ReviewReplanProposal;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.List;
import java.util.Objects;

public final class ReviewReplanProposalInteractor implements ReviewReplanProposal {
    private final ExpeditionRepository plans;
    private final ReplanProposalRepository proposals;
    private final Clock clock;

    public ReviewReplanProposalInteractor(
            ExpeditionRepository plans,
            ReplanProposalRepository proposals,
            Clock clock
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.proposals = Objects.requireNonNull(proposals, "proposals");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void accept(ProposalId proposalId, PersonId responsible) {
        ReplanProposal proposal = require(proposalId);
        Expedition original = plans.require(proposal.originalId());
        requireResponsible(original, responsible);
        if (original.status() != ExpeditionStatus.APPROVED) {
            throw new InvalidExpeditionTransition(original.status(), "accept replan");
        }
        proposal.accept(responsible, clock.now());
        plans.save(proposal.suggested());
        proposals.save(proposal);
    }

    @Override
    public void reject(ProposalId proposalId, PersonId responsible) {
        ReplanProposal proposal = require(proposalId);
        requireResponsible(plans.require(proposal.originalId()), responsible);
        proposal.reject(responsible, clock.now());
        proposals.save(proposal);
    }

    @Override
    public List<ProposalSnapshot> of(ExpeditionId expeditionId) {
        return proposals.of(Objects.requireNonNull(expeditionId, "expedition id")).stream()
                .map(ProposalSnapshot::of)
                .toList();
    }

    private ReplanProposal require(ProposalId proposalId) {
        return proposals.find(Objects.requireNonNull(proposalId, "proposal id"))
                .orElseThrow(() -> new InvalidValue("unknown proposal: " + proposalId));
    }

    private static void requireResponsible(Expedition original, PersonId responsible) {
        Objects.requireNonNull(responsible, "responsible");
        if (!original.charter().isResponsible(responsible)) {
            throw new InvalidValue("replan must be decided by a responsible");
        }
    }
}
