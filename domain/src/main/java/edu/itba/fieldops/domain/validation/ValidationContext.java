package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.catalog.Consumables;
import edu.itba.fieldops.domain.catalog.Instruments;
import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.catalog.Permits;
import edu.itba.fieldops.domain.catalog.Vehicles;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.OccupyingExpeditions;

import java.util.Objects;

public record ValidationContext(Expedition expedition, Catalogs catalogs, OccupyingExpeditions occupying) {
    public ValidationContext {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalogs, "catalogs");
        Objects.requireNonNull(occupying, "occupying expeditions");
    }

    public People people() {
        return catalogs.people();
    }

    public Vehicles vehicles() {
        return catalogs.vehicles();
    }

    public Instruments instruments() {
        return catalogs.instruments();
    }

    public Consumables consumables() {
        return catalogs.consumables();
    }

    public Permits permits() {
        return catalogs.permits();
    }

    public BookableResources bookable() {
        return catalogs.bookable();
    }
}
