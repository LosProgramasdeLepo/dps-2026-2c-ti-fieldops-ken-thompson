package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.adapters.jpa.JpaPages;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.usecase.expedition.ExpeditionRepository;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JpaExpeditionRepository implements ExpeditionRepository {
    private final ExpeditionJpaRepository expeditions;
    private final ExpeditionRegistrationJpaRepository registrations;
    private final StoredExpeditions stored;
    private final TransactionOperations transactions;

    public JpaExpeditionRepository(
            ExpeditionJpaRepository expeditions,
            ExpeditionRegistrationJpaRepository registrations,
            TransactionOperations transactions
    ) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
        this.stored = new StoredExpeditions(expeditions);
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public ExpeditionId nextId() {
        return new ExpeditionId(UUID.randomUUID());
    }

    @Override
    public ActivityId nextActivityId() {
        return new ActivityId(UUID.randomUUID());
    }

    @Override
    public void save(Expedition expedition) {
        Objects.requireNonNull(expedition, "expedition");
        transactions.executeWithoutResult(status -> {
            stored.save(expedition);
            UUID id = expedition.id().value();
            if (!registrations.existsById(id)) {
                registrations.save(new ExpeditionRegistrationEntity(id));
            }
        });
    }

    @Override
    public Optional<Expedition> find(ExpeditionId id) {
        Objects.requireNonNull(id, "expedition id");
        return transactions.execute(status -> expeditions.findRegisteredById(id.value()).map(ExpeditionEntity::toDomain));
    }

    @Override
    public List<Expedition> all() {
        return transactions.execute(status -> expeditions.findAllRegistered().stream()
                .map(ExpeditionEntity::toDomain)
                .toList());
    }

    @Override
    public Page<Expedition> all(PageRequest request) {
        return transactions.execute(status -> JpaPages.page(
                expeditions.findAllRegistered(JpaPages.unsorted(request)), request, ExpeditionEntity::toDomain
        ));
    }
}
