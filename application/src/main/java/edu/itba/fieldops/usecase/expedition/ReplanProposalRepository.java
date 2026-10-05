package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.ProposalId;

import java.util.List;
import java.util.Optional;

public interface ReplanProposalRepository {
    ProposalId nextId();

    void save(ReplanProposal proposal);

    Optional<ReplanProposal> find(ProposalId id);

    List<ReplanProposal> of(ExpeditionId originalId);
}
