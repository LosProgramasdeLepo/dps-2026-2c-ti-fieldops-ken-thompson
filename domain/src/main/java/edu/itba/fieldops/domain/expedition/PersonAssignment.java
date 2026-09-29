package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalog;
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
        return Optional.of(new TemporalBooking(TemporalBooking.Kind.PERSON, personId.value(), activityId, window));
    }

    @Override
    public Optional<String> unknownIn(Catalog catalog) {
        return catalog.person(personId).isEmpty() ? Optional.of("person " + personId) : Optional.empty();
    }
}
