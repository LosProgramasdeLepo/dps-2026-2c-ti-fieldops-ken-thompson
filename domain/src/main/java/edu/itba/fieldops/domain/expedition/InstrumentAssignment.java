package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Objects;
import java.util.Optional;

public record InstrumentAssignment(ActivityId activityId, InstrumentId instrumentId) implements BookableAssignment {
    public InstrumentAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(instrumentId, "instrument id");
    }

    @Override
    public TemporalBooking booking(TimePeriod window) {
        return new TemporalBooking.InstrumentBooking(instrumentId, activityId, window);
    }

    @Override
    public Optional<String> unknownIn(Catalogs catalogs) {
        return catalogs.instruments().instrument(instrumentId).isEmpty()
                ? Optional.of("instrument " + instrumentId)
                : Optional.empty();
    }

    @Override
    public void fileInto(Assignments assignments) {
        assignments.file(this);
    }

    @Override
    public void withdrawFrom(Assignments assignments) {
        assignments.withdraw(this);
    }
}
