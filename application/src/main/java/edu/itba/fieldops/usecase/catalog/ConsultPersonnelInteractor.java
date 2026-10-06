package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.Objects;

public final class ConsultPersonnelInteractor implements ConsultPersonnel {
    private final CertificationRegistry certifications;
    private final PersonRegistry people;

    public ConsultPersonnelInteractor(CertificationRegistry certifications, PersonRegistry people) {
        this.certifications = Objects.requireNonNull(certifications, "certifications");
        this.people = Objects.requireNonNull(people, "people");
    }

    @Override
    public Page<Certification> certifications(PageRequest request) {
        return certifications.certifications(request);
    }

    @Override
    public Certification certification(CertificationId certificationId) {
        return certifications.require(certificationId);
    }

    @Override
    public Page<Person> people(PageRequest request) {
        return people.people(request);
    }

    @Override
    public Person person(PersonId personId) {
        return people.require(personId);
    }
}
