package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Certifications;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.Objects;

public interface CertificationRegistry extends Certifications {
    CertificationId nextCertificationId();

    void save(Certification certification);

    Page<Certification> certifications(PageRequest request);

    default Certification require(CertificationId id) {
        return certification(Objects.requireNonNull(id, "certification id"))
                .orElseThrow(() -> new UnknownResource("certification", id.value().toString()));
    }
}
