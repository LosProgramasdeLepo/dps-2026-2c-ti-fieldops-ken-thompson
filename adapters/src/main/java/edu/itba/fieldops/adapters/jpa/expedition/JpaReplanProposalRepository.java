package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.jpa.JpaPages;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.SQL)
@Transactional(readOnly = true)
public class JpaReplanProposalRepository implements ReplanProposalRepository {
    private final ProposalJpaRepository proposals;
    private final StoredExpeditions stored;

    public JpaReplanProposalRepository(
            ProposalJpaRepository proposals,
            ExpeditionJpaRepository expeditions) {
        this.proposals = Objects.requireNonNull(proposals, "proposals");
        this.stored = new StoredExpeditions(expeditions);
    }

    @Override
    public ProposalId nextId() {
        return new ProposalId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(ReplanProposal proposal) {
        Objects.requireNonNull(proposal, "proposal");
        stored.save(proposal.suggested());
        proposals.save(new ProposalEntity(proposal.state()));
    }

    @Override
    public Optional<ReplanProposal> find(ProposalId id) {
        Objects.requireNonNull(id, "proposal id");
        return proposals.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<ReplanProposal> forIncident(IncidentId id) {
        Objects.requireNonNull(id, "incident id");
        return proposals.findByIncidentId(id.value()).map(this::toDomain);
    }

    @Override
    public Page<ReplanProposal> of(ExpeditionId originalId, PageRequest request) {
        Objects.requireNonNull(originalId, "expedition id");
        return JpaPages.page(proposals.findByOriginalId(originalId.value(), JpaPages.pageable(request)), request, this::toDomain);
    }

    private ReplanProposal toDomain(ProposalEntity entity) {
        return entity.toDomain(stored.find(new ExpeditionId(entity.suggestedId())).orElseThrow());
    }
}
