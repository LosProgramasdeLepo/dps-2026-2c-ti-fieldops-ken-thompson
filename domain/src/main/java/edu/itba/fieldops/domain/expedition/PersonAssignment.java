package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Objects;
import java.util.Optional;

public record PersonAssignment(ActivityId activityId, PersonId personId) implements Assignment {
    public PersonAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(personId, "person id");
    }

    @Override
    public Optional<TemporalBooking> booking(TimePeriod window) {
        return Optional.of(new TemporalBooking.PersonBooking(personId, activityId, window));
    }

    @Override
    public Optional<String> unknownIn(Catalogs catalogs) {
        return catalogs.people().person(personId).isEmpty() ? Optional.of("person " + personId) : Optional.empty();
    }

    @Override
    public void fileInto(Assignments assignments) {
        assignments.file(this);
    }

    @Override
    public boolean withdrawFrom(Assignments assignments) {
        return assignments.withdraw(this);
    }
}
