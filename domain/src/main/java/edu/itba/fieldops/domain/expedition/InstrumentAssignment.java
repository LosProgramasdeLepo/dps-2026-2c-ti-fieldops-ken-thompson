package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.BookableId;
import edu.itba.fieldops.domain.identity.InstrumentId;

import java.util.Objects;
import java.util.Optional;

public record InstrumentAssignment(ActivityId activityId, InstrumentId instrumentId) implements BookableAssignment {
    public InstrumentAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(instrumentId, "instrument id");
    }

    @Override
    public BookableId resourceId() {
        return instrumentId;
    }

    @Override
    public Optional<Instrument> resourceIn(BookableResources resources) {
        return resources.instruments().instrument(instrumentId);
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
