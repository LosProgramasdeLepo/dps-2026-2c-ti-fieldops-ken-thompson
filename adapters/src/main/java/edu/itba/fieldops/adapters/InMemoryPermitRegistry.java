package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.PermitRegistry;
import edu.itba.fieldops.domain.identity.PermitId;
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
public class InMemoryPermitRegistry implements PermitRegistry {
    private final Map<PermitId, Permit> permits = new LinkedHashMap<>();

    @Override
    public PermitId nextPermitId() {
        return new PermitId(UUID.randomUUID());
    }

    @Override
    public synchronized void save(Permit permit) {
        Objects.requireNonNull(permit, "permit");
        permits.put(permit.id(), permit);
    }

    @Override
    public synchronized Optional<Permit> permit(PermitId id) {
        Objects.requireNonNull(id, "permit id");
        return Optional.ofNullable(permits.get(id));
    }

    @Override
    public synchronized Page<Permit> permits(PageRequest request) {
        return InMemoryPages.slice(permits.values(), request);
    }
}
