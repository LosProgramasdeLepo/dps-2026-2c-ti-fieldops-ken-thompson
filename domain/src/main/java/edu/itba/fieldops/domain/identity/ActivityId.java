package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record ActivityId(UUID value) {
    public ActivityId {
        Objects.requireNonNull(value, "activity id");
    }
}
