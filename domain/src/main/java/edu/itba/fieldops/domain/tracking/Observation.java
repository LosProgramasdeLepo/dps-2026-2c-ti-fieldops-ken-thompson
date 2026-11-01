package edu.itba.fieldops.domain.tracking;


import java.time.Instant;
import java.util.Objects;

public record Observation(ObservationText text, Instant at) {
    public Observation {
        Objects.requireNonNull(text, "observation");
        Objects.requireNonNull(at, "observation time");
    }
}
