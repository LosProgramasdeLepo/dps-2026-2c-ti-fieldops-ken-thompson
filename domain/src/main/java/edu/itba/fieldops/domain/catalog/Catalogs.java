package edu.itba.fieldops.domain.catalog;

import java.util.Objects;

public record Catalogs(BookableResources bookable, Consumables consumables, Permits permits) {
    public Catalogs {
        Objects.requireNonNull(bookable, "bookable resources");
        Objects.requireNonNull(consumables, "consumables");
        Objects.requireNonNull(permits, "permits");
    }
}
