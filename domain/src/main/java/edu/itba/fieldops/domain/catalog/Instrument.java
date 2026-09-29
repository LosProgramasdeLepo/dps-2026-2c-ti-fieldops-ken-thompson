package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Objects;

public final class Instrument {
    private final InstrumentId id;
    private final InstrumentKind kind;
    private final Availability availability;

    public Instrument(InstrumentId id, InstrumentKind kind, Availability availability) {
        this.id = Objects.requireNonNull(id, "instrument id");
        this.kind = Objects.requireNonNull(kind, "instrument kind");
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    public InstrumentId id() {
        return id;
    }

    public InstrumentKind kind() {
        return kind;
    }

    public boolean availableDuring(TimePeriod period) {
        return availability.covers(period);
    }
}
