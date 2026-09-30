package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.PersonId;

public interface PersonRegistry extends People {
    PersonId nextPersonId();

    void save(Person person);
}
