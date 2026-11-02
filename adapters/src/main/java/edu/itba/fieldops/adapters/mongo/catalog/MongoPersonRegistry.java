package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.PersonRegistry;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.MONGO)
@Transactional(readOnly = true)
public class MongoPersonRegistry implements PersonRegistry {
    private final StoredCatalog<PersonDocument, Person> stored;

    MongoPersonRegistry(PersonMongoRepository repository, RegistrationSequence sequence) {
        this.stored = new StoredCatalog<>(repository, sequence, PersonDocument.COLLECTION, PersonDocument::toDomain);
    }

    @Override
    public PersonId nextPersonId() {
        return new PersonId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(Person person) {
        Objects.requireNonNull(person, "person");
        stored.save(person.id().value(), order -> PersonDocument.of(person, order));
    }

    @Override
    public Optional<Person> person(PersonId id) {
        return stored.find(Objects.requireNonNull(id, "person id").value());
    }

    @Override
    public List<Person> people() {
        return stored.all();
    }

    @Override
    public Page<Person> people(PageRequest request) {
        return stored.page(request);
    }
}
