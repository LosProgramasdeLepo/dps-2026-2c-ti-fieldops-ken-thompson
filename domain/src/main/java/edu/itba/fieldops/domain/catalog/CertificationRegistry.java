package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.CertificationId;

public interface CertificationRegistry extends Certifications {
    CertificationId nextCertificationId();

    void save(Certification certification);
}
