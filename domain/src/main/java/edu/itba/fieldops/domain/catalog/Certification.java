package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.shared.Texts;

import java.util.Objects;

public record Certification(CertificationId id, String name) {
    public Certification {
        Objects.requireNonNull(id, "certification id");
        name = Texts.required(name, "certification name");
    }
}
