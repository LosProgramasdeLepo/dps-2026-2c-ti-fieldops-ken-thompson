package edu.itba.fieldops.domain.catalog;

import java.util.Objects;

public record Catalogs(BookableResources bookable, Consumables consumables, Permits permits) {
    public Catalogs {
        Objects.requireNonNull(bookable, "bookable resources");
        Objects.requireNonNull(consumables, "consumables");
        Objects.requireNonNull(permits, "permits");
    }

    public People people() {
        return bookable.people();
    }

    public Vehicles vehicles() {
        return bookable.vehicles();
    }

    public Instruments instruments() {
        return bookable.instruments();
    }
}
