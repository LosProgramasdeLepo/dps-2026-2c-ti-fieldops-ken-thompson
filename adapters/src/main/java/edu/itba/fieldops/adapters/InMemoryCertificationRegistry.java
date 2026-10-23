package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.CertificationRegistry;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryCertificationRegistry implements CertificationRegistry {
    private final Map<CertificationId, Certification> certifications = new LinkedHashMap<>();

    @Override
    public CertificationId nextCertificationId() {
        return new CertificationId(UUID.randomUUID());
    }

    @Override
    public void save(Certification certification) {
        Objects.requireNonNull(certification, "certification");
        certifications.put(certification.id(), certification);
    }

    @Override
    public Optional<Certification> certification(CertificationId id) {
        Objects.requireNonNull(id, "certification id");
        return Optional.ofNullable(certifications.get(id));
    }

    @Override
    public Page<Certification> certifications(PageRequest request) {
        return InMemoryPages.slice(certifications.values(), request);
    }
}
