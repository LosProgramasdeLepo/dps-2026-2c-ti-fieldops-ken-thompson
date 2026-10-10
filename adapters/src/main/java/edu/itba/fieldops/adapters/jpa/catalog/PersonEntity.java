package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.adapters.jpa.StoredPeriod;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.PersonId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "people")
public class PersonEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToMany
    @JoinTable(
            name = "person_certifications",
            joinColumns = @JoinColumn(name = "person_id"),
            inverseJoinColumns = @JoinColumn(name = "certification_id")
    )
    @OrderColumn(name = "position")
    private List<CertificationEntity> certifications = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "person_availability", joinColumns = @JoinColumn(name = "person_id"))
    @OrderColumn(name = "position")
    private List<StoredPeriod> availability = new ArrayList<>();

    @Column(name = "registration_order", insertable = false, updatable = false)
    private Long registrationOrder;

    protected PersonEntity() {
    }

    PersonEntity(Person person, List<CertificationEntity> certifications) {
        this.id = person.id().value();
        this.name = person.name();
        this.certifications = new ArrayList<>(certifications);
        this.availability = new ArrayList<>(StoredPeriod.of(person.availability()));
    }

    public Person toDomain() {
        return new Person(
                new PersonId(id),
                name,
                certifications.stream().map(CertificationEntity::toDomain).toList(),
                StoredPeriod.availability(availability)
        );
    }
}
