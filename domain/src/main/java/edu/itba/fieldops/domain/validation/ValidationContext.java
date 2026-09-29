package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.OccupyingExpeditions;

import java.util.Objects;

public record ValidationContext(Expedition expedition, Catalog catalog, OccupyingExpeditions occupying) {
    public ValidationContext {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(occupying, "occupying expeditions");
    }
}
