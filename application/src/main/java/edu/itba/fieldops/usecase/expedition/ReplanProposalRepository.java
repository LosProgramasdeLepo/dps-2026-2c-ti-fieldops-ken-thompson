package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.Optional;

public interface ReplanProposalRepository {
    ProposalId nextId();

    void save(ReplanProposal proposal);

    Optional<ReplanProposal> find(ProposalId id);

    Page<ReplanProposal> of(ExpeditionId originalId, PageRequest request);
}
