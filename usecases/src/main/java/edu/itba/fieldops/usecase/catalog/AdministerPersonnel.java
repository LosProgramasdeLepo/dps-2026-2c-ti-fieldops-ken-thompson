package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.PersonId;

import java.util.List;

public interface AdministerPersonnel {
    CertificationId registerCertification(String name);

    PersonId registerPerson(String name, List<CertificationId> certifications, Availability availability);

    void certify(PersonId personId, CertificationId certificationId);

    void changeAvailability(PersonId personId, Availability availability);
}
