package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.Objects;

public interface PersonRegistry extends People {
    PersonId nextPersonId();

    void save(Person person);

    Page<Person> people(PageRequest request);

    default Person require(PersonId id) {
        return person(Objects.requireNonNull(id, "person id"))
                .orElseThrow(() -> new UnknownResource("person", id.value().toString()));
    }
}
