package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record InstrumentId(UUID value) {
    public InstrumentId {
        Objects.requireNonNull(value, "instrument id");
    }
}
