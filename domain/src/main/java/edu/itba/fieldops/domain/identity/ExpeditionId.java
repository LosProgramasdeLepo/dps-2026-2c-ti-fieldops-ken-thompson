package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record ExpeditionId(UUID value) {
    public ExpeditionId {
        Objects.requireNonNull(value, "expedition id");
    }
}
