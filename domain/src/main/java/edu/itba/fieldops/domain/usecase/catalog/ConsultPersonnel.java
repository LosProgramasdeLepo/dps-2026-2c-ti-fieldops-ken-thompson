package edu.itba.fieldops.domain.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;

public interface ConsultPersonnel {
    Page<Certification> certifications(PageRequest request);

    Certification certification(CertificationId certificationId);

    Page<Person> people(PageRequest request);

    Person person(PersonId personId);
}
