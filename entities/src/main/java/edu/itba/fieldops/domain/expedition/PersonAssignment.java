package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.BookableId;
import edu.itba.fieldops.domain.identity.PersonId;

import java.util.Objects;
import java.util.Optional;

public record PersonAssignment(ActivityId activityId, PersonId personId) implements BookableAssignment {
    public PersonAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(personId, "person id");
    }

    @Override
    public BookableId resourceId() {
        return personId;
    }

    @Override
    public Optional<Person> resourceIn(BookableResources resources) {
        return resources.people().person(personId);
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
