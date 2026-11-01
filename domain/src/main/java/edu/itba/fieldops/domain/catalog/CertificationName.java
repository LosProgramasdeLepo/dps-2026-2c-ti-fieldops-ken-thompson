package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Texts;

public record CertificationName(String value) {
    public CertificationName {
        value = Texts.required(value, "certification name");
    }
}
