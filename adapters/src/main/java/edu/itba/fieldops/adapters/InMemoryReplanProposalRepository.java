package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.MEMORY)
public class InMemoryReplanProposalRepository implements ReplanProposalRepository {
    private final Map<ProposalId, ReplanProposal> proposals = new LinkedHashMap<>();

    @Override
    public ProposalId nextId() {
        return new ProposalId(UUID.randomUUID());
    }

    @Override
    public synchronized void save(ReplanProposal proposal) {
        Objects.requireNonNull(proposal, "proposal");
        proposals.put(proposal.id(), proposal);
    }

    @Override
    public synchronized Optional<ReplanProposal> find(ProposalId id) {
        Objects.requireNonNull(id, "proposal id");
        return Optional.ofNullable(proposals.get(id));
    }

    @Override
    public synchronized Optional<ReplanProposal> forIncident(IncidentId id) {
        Objects.requireNonNull(id, "incident id");
        return proposals.values().stream().filter(proposal -> proposal.incident().id().equals(id)).findFirst();
    }

    @Override
    public synchronized Page<ReplanProposal> of(ExpeditionId originalId, PageRequest request) {
        Objects.requireNonNull(originalId, "expedition id");
        return InMemoryPages.slice(
                proposals.values().stream().filter(proposal -> proposal.originalId().equals(originalId)).toList(),
                request
        );
    }
}
