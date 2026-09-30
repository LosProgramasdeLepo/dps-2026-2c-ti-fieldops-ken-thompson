package edu.itba.fieldops.domain.expedition.usecase;

import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;

import java.util.List;

public interface ReviewReplanProposal {
    void accept(ProposalId proposalId, PersonId responsible);

    void reject(ProposalId proposalId, PersonId responsible);

    List<ReplanProposal> of(ExpeditionId expeditionId);
}
