package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ProposalReviewer;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.usecase.expedition.ProposalSnapshot;
import edu.itba.fieldops.domain.usecase.expedition.ReviewReplanProposal;

import java.util.Objects;

public final class ReviewReplanProposalInteractor implements ReviewReplanProposal {
    private final ExpeditionRepository plans;
    private final ReplanProposalRepository proposals;
    private final ProposalReviewer reviewer;

    public ReviewReplanProposalInteractor(
            ExpeditionRepository plans,
            ReplanProposalRepository proposals,
            Clock clock
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.proposals = Objects.requireNonNull(proposals, "proposals");
        this.reviewer = new ProposalReviewer(plans, clock);
    }

    @Override
    public void accept(ProposalId proposalId, PersonId responsible) {
        ReplanProposal proposal = require(proposalId);
        reviewer.accept(proposal, responsible);
        plans.save(proposal.suggested());
        proposals.save(proposal);
    }

    @Override
    public void reject(ProposalId proposalId, PersonId responsible) {
        ReplanProposal proposal = require(proposalId);
        reviewer.reject(proposal, responsible);
        proposals.save(proposal);
    }

    @Override
    public Page<ProposalSnapshot> of(ExpeditionId expeditionId, PageRequest request) {
        plans.require(expeditionId);
        return proposals.of(expeditionId, request).map(ProposalSnapshot::of);
    }

    @Override
    public ProposalSnapshot of(ProposalId proposalId) {
        return ProposalSnapshot.of(require(proposalId));
    }

    private ReplanProposal require(ProposalId proposalId) {
        return proposals.find(Objects.requireNonNull(proposalId, "proposal id"))
                .orElseThrow(() -> new UnknownResource("proposal", proposalId.value().toString()));
    }
}
