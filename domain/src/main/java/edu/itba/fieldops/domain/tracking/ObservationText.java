package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.shared.Texts;

public record ObservationText(String text) {
    public ObservationText {
        text = Texts.required(text, "observation");
    }
}
