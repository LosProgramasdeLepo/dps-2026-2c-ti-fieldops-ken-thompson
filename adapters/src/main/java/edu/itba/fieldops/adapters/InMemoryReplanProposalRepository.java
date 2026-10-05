package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.usecase.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.ProposalId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    public List<ReplanProposal> of(ExpeditionId originalId) {
        Objects.requireNonNull(originalId, "expedition id");
        List<ReplanProposal> matching = new ArrayList<>();
        for (ReplanProposal proposal : proposals.values()) {
            if (proposal.originalId().equals(originalId)) {
                matching.add(proposal);
            }
        }
        return List.copyOf(matching);
    }
}
