package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.catalog.usecase.AdministerPersonnel;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.InvalidValue;

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
        people.save(new Person(id, name, certificationIds.stream().map(this::requireCertification).toList(), availability));
        return id;
    }

    @Override
    public void certify(PersonId personId, CertificationId certificationId) {
        people.save(requirePerson(personId).certified(requireCertification(certificationId)));
    }

    @Override
    public void changeAvailability(PersonId personId, Availability availability) {
        people.save(requirePerson(personId).withAvailability(availability));
    }

    private Certification requireCertification(CertificationId certificationId) {
        return certifications.certification(certificationId)
                .orElseThrow(() -> new InvalidValue("unknown certification: " + certificationId));
    }

    private Person requirePerson(PersonId personId) {
        return people.person(personId)
                .orElseThrow(() -> new InvalidValue("unknown person: " + personId));
    }
}
