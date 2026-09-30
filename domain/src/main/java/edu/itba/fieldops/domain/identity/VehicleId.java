package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record VehicleId(UUID value) implements BookableId {
    public VehicleId {
        Objects.requireNonNull(value, "vehicle id");
    }

    @Override
    public String label() {
        return "vehicle " + value;
    }
}
