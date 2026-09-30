package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record InstrumentId(UUID value) implements BookableId {
    public InstrumentId {
        Objects.requireNonNull(value, "instrument id");
    }

    @Override
    public String label() {
        return "instrument " + value;
    }
}
