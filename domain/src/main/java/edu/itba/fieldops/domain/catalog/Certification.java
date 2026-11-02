package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.CertificationId;

import java.util.Objects;

public record Certification(CertificationId id, CertificationName name) {
    public Certification {
        Objects.requireNonNull(id, "certification id");
        Objects.requireNonNull(name, "certification name");
    }
}
