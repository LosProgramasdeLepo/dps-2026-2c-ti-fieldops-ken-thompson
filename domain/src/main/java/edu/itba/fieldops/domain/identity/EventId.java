package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record EventId(UUID value) {
    public EventId {
        Objects.requireNonNull(value, "event id");
    }
}
