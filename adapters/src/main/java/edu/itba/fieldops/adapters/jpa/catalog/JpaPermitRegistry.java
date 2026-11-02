package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.PermitRegistry;
import edu.itba.fieldops.domain.identity.PermitId;
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
public class JpaPermitRegistry implements PermitRegistry {
    private final StoredCatalog<PermitEntity, Permit> stored;

    public JpaPermitRegistry(PermitJpaRepository repository) {
        this.stored = new StoredCatalog<>(repository, PermitEntity::toDomain);
    }

    @Override
    public PermitId nextPermitId() {
        return new PermitId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(Permit permit) {
        Objects.requireNonNull(permit, "permit");
        stored.save(new PermitEntity(permit));
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
