package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record VehicleId(UUID value) {
    public VehicleId {
        Objects.requireNonNull(value, "vehicle id");
    }
}
