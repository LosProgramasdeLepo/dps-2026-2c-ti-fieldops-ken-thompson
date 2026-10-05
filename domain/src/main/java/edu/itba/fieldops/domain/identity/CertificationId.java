package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record CertificationId(UUID value) {
    public CertificationId {
        Objects.requireNonNull(value, "certification id");
    }
}
