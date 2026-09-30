package edu.itba.fieldops.domain.catalog;

import java.util.Objects;

public record BookableResources(People people, Vehicles vehicles, Instruments instruments) {
    public BookableResources {
        Objects.requireNonNull(people, "people");
        Objects.requireNonNull(vehicles, "vehicles");
        Objects.requireNonNull(instruments, "instruments");
    }
}
