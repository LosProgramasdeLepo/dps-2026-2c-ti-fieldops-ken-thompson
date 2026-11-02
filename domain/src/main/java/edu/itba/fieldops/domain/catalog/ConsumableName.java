package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Texts;

public record ConsumableName(String value) {
    public ConsumableName {
        value = Texts.required(value, "consumable name");
    }
}
