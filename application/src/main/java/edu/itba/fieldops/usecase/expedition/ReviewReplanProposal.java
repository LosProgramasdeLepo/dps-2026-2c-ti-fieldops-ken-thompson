package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

public interface ReviewReplanProposal {
    void accept(ProposalId proposalId, PersonId responsible);

    void reject(ProposalId proposalId, PersonId responsible);

    Page<ProposalSnapshot> of(ExpeditionId expeditionId, PageRequest request);

    ProposalSnapshot of(ProposalId proposalId);
}
