package edu.itba.fieldops.domain.expedition.usecase;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;

import java.util.List;

public interface ReviewReplanProposal {
    void accept(ProposalId proposalId, PersonId responsible);

    void reject(ProposalId proposalId, PersonId responsible);

    List<ProposalSnapshot> of(ExpeditionId expeditionId);
}
