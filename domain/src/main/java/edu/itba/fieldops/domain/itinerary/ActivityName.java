package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.Texts;

public record ActivityName(String value) {
    public ActivityName {
        value = Texts.required(value, "activity name");
    }
}
