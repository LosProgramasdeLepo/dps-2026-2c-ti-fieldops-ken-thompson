package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.CertificationRegistry;
import edu.itba.fieldops.domain.identity.CertificationId;
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
public class JpaCertificationRegistry implements CertificationRegistry {
    private final StoredCatalog<CertificationEntity, Certification> stored;

    public JpaCertificationRegistry(CertificationJpaRepository repository) {
        this.stored = new StoredCatalog<>(repository, CertificationEntity::toDomain);
    }

    @Override
    public CertificationId nextCertificationId() {
        return new CertificationId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(Certification certification) {
        Objects.requireNonNull(certification, "certification");
        stored.save(new CertificationEntity(certification));
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
