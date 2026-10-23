package edu.itba.fieldops.domain.support;

import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.PersonId;

import java.util.List;
import java.util.Optional;

public final class StoredPeople implements People {
    private final List<Person> people;

    private StoredPeople(List<Person> people) {
        this.people = people;
    }

    public static StoredPeople of(Person... people) {
        return new StoredPeople(List.of(people));
    }

    @Override
    public Optional<Person> person(PersonId id) {
        return people.stream().filter(person -> person.id().equals(id)).findFirst();
    }

    @Override
    public List<Person> people() {
        return people;
    }
}
