package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Certifications;
import edu.itba.fieldops.domain.identity.CertificationId;

public interface CertificationRegistry extends Certifications {
    CertificationId nextCertificationId();

    void save(Certification certification);
}
