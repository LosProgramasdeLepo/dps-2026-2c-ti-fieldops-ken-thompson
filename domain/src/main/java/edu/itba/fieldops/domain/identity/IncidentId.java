package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record IncidentId(UUID value) {
    public IncidentId {
        Objects.requireNonNull(value, "incident id");
    }
}
