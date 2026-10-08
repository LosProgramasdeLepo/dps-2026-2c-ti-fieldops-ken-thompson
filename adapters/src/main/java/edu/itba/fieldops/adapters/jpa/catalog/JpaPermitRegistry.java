package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.usecase.catalog.PermitRegistry;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JpaPermitRegistry implements PermitRegistry {
    private final StoredCatalog<PermitEntity, Permit> stored;

    public JpaPermitRegistry(PermitJpaRepository repository, TransactionOperations transactions) {
        this.stored = new StoredCatalog<>(repository, PermitEntity::toDomain, transactions);
    }

    @Override
    public PermitId nextPermitId() {
        return new PermitId(UUID.randomUUID());
    }

    @Override
    public void save(Permit permit) {
        Objects.requireNonNull(permit, "permit");
        stored.save(() -> new PermitEntity(permit));
    }

    @Override
    public Optional<Permit> permit(PermitId id) {
        return stored.find(Objects.requireNonNull(id, "permit id").value());
    }

    @Override
    public Page<Permit> permits(PageRequest request) {
        return stored.page(request);
    }
}
