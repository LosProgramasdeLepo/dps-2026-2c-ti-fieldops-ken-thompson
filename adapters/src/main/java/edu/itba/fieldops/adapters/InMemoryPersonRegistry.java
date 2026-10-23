package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.PersonRegistry;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryPersonRegistry implements PersonRegistry {
    private final Map<PersonId, Person> people = new LinkedHashMap<>();

    @Override
    public PersonId nextPersonId() {
        return new PersonId(UUID.randomUUID());
    }

    @Override
    public void save(Person person) {
        Objects.requireNonNull(person, "person");
        people.put(person.id(), person);
    }

    @Override
    public Optional<Person> person(PersonId id) {
        Objects.requireNonNull(id, "person id");
        return Optional.ofNullable(people.get(id));
    }

    @Override
    public List<Person> people() {
        return List.copyOf(people.values());
    }

    @Override
    public Page<Person> people(PageRequest request) {
        return InMemoryPages.slice(people.values(), request);
    }
}
