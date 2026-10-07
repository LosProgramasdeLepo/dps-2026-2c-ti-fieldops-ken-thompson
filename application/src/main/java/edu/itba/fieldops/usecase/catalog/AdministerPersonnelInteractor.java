package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.PersonId;

import java.util.List;
import java.util.Objects;

public final class AdministerPersonnelInteractor implements AdministerPersonnel {
    private final CertificationRegistry certifications;
    private final PersonRegistry people;

    public AdministerPersonnelInteractor(CertificationRegistry certifications, PersonRegistry people) {
        this.certifications = Objects.requireNonNull(certifications, "certifications");
        this.people = Objects.requireNonNull(people, "people");
    }

    @Override
    public CertificationId registerCertification(String name) {
        CertificationId id = certifications.nextCertificationId();
        certifications.save(new Certification(id, name));
        return id;
    }

    @Override
    public PersonId registerPerson(String name, List<CertificationId> certificationIds, Availability availability) {
        PersonId id = people.nextPersonId();
        people.save(new Person(id, name, certificationIds.stream().map(certifications::require).toList(), availability));
        return id;
    }

    @Override
    public void certify(PersonId personId, CertificationId certificationId) {
        people.save(people.require(personId).certified(certifications.require(certificationId)));
    }

    @Override
    public void changeAvailability(PersonId personId, Availability availability) {
        people.save(people.require(personId).withAvailability(availability));
    }
}
