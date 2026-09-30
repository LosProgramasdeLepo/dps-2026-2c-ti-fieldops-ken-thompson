package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.PersonId;

import java.util.List;
import java.util.Optional;

public interface People {
    Optional<Person> person(PersonId id);

    List<Person> people();
}
