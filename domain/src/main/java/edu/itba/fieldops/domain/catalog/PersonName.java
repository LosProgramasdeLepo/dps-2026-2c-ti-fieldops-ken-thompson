package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Texts;

public record PersonName(String value) {
    public PersonName {
        value = Texts.required(value, "person name");
    }
}
