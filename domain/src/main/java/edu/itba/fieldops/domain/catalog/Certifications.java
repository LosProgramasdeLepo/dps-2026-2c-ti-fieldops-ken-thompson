package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.CertificationId;

import java.util.Optional;

public interface Certifications {
    Optional<Certification> certification(CertificationId id);
}
