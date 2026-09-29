package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record PermitId(UUID value) {
    public PermitId {
        Objects.requireNonNull(value, "permit id");
    }
}
