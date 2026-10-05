package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.PersonId;

public interface PersonRegistry extends People {
    PersonId nextPersonId();

    void save(Person person);
}
