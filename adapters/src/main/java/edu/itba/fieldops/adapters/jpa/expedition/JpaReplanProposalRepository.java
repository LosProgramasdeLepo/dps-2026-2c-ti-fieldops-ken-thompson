package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.adapters.jpa.JpaPages;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.usecase.expedition.ReplanProposalRepository;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JpaReplanProposalRepository implements ReplanProposalRepository {
    private final ProposalJpaRepository proposals;
    private final StoredExpeditions stored;
    private final TransactionOperations transactions;

    public JpaReplanProposalRepository(
            ProposalJpaRepository proposals,
            ExpeditionJpaRepository expeditions,
            TransactionOperations transactions
    ) {
        this.proposals = Objects.requireNonNull(proposals, "proposals");
        this.stored = new StoredExpeditions(expeditions);
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public ProposalId nextId() {
        return new ProposalId(UUID.randomUUID());
    }

    @Override
    public void save(ReplanProposal proposal) {
        Objects.requireNonNull(proposal, "proposal");
        transactions.executeWithoutResult(status -> {
            stored.save(proposal.suggested());
            proposals.save(new ProposalEntity(proposal));
        });
    }

    @Override
    public Optional<ReplanProposal> find(ProposalId id) {
        Objects.requireNonNull(id, "proposal id");
        return transactions.execute(status -> proposals.findById(id.value()).map(this::toDomain));
    }

    @Override
    public Page<ReplanProposal> of(ExpeditionId originalId, PageRequest request) {
        Objects.requireNonNull(originalId, "expedition id");
        return transactions.execute(status -> JpaPages.page(
                proposals.findByOriginalId(originalId.value(), JpaPages.pageable(request)), request, this::toDomain
        ));
    }

    private ReplanProposal toDomain(ProposalEntity entity) {
        return entity.toDomain(stored.find(new ExpeditionId(entity.suggestedId())).orElseThrow());
    }
}
