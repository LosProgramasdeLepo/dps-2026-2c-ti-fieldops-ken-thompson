package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.usecase.catalog.CertificationRegistry;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JpaCertificationRegistry implements CertificationRegistry {
    private final StoredCatalog<CertificationEntity, Certification> stored;

    public JpaCertificationRegistry(CertificationJpaRepository repository, TransactionOperations transactions) {
        this.stored = new StoredCatalog<>(repository, CertificationEntity::toDomain, transactions);
    }

    @Override
    public CertificationId nextCertificationId() {
        return new CertificationId(UUID.randomUUID());
    }

    @Override
    public void save(Certification certification) {
        Objects.requireNonNull(certification, "certification");
        stored.save(() -> new CertificationEntity(certification));
    }

    @Override
    public Optional<Certification> certification(CertificationId id) {
        return stored.find(Objects.requireNonNull(id, "certification id").value());
    }

    @Override
    public Page<Certification> certifications(PageRequest request) {
        return stored.page(request);
    }
}
