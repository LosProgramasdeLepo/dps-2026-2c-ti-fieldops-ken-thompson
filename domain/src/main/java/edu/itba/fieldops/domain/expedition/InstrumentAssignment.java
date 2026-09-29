package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Objects;
import java.util.Optional;

public record InstrumentAssignment(ActivityId activityId, InstrumentId instrumentId) implements Assignment {
    public InstrumentAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(instrumentId, "instrument id");
    }

    @Override
    public Optional<TemporalBooking> booking(TimePeriod window) {
        return Optional.of(new TemporalBooking.InstrumentBooking(instrumentId, activityId, window));
    }

    @Override
    public Optional<String> unknownIn(Catalog catalog) {
        return catalog.instrument(instrumentId).isEmpty() ? Optional.of("instrument " + instrumentId) : Optional.empty();
    }
}
