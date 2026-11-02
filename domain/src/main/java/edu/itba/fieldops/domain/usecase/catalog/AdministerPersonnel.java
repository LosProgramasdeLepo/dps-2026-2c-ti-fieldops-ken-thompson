package edu.itba.fieldops.domain.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.CertificationName;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.PersonId;

import java.util.List;

public interface AdministerPersonnel {
    CertificationId registerCertification(CertificationName name);

    PersonId registerPerson(PersonName name, List<CertificationId> certifications, Availability availability);

    void certify(PersonId personId, CertificationId certificationId);

    void changeAvailability(PersonId personId, Availability availability);
}
