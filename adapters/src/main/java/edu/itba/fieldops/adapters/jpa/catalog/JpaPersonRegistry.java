package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.adapters.PersistenceProfiles;
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
@Profile(PersistenceProfiles.SQL)
@Transactional(readOnly = true)
public class JpaPersonRegistry implements PersonRegistry {
    private final StoredCatalog<PersonEntity, Person> stored;
    private final CertificationJpaRepository certifications;

    public JpaPersonRegistry(
            PersonJpaRepository repository,
            CertificationJpaRepository certifications) {
        this.stored = new StoredCatalog<>(repository, PersonEntity::toDomain);
        this.certifications = Objects.requireNonNull(certifications, "certifications");
    }

    @Override
    public PersonId nextPersonId() {
        return new PersonId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(Person person) {
        Objects.requireNonNull(person, "person");
        stored.save(new PersonEntity(person, heldBy(person)));
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

    private List<CertificationEntity> heldBy(Person person) {
        return person.certifications().stream()
                .map(certification -> certifications.getReferenceById(certification.id().value()))
                .toList();
    }
}
