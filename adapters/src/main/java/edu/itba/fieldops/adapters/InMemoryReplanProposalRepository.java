package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.usecase.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryReplanProposalRepository implements ReplanProposalRepository {
    private final Map<ProposalId, ReplanProposal> proposals = new LinkedHashMap<>();

    @Override
    public ProposalId nextId() {
        return new ProposalId(UUID.randomUUID());
    }

    @Override
    public void save(ReplanProposal proposal) {
        Objects.requireNonNull(proposal, "proposal");
        proposals.put(proposal.id(), proposal);
    }

    @Override
    public Optional<ReplanProposal> find(ProposalId id) {
        Objects.requireNonNull(id, "proposal id");
        return Optional.ofNullable(proposals.get(id));
    }

    @Override
    public Page<ReplanProposal> of(ExpeditionId originalId, PageRequest request) {
        Objects.requireNonNull(originalId, "expedition id");
        return InMemoryPages.slice(
                proposals.values().stream().filter(proposal -> proposal.originalId().equals(originalId)).toList(),
                request
        );
    }
}
