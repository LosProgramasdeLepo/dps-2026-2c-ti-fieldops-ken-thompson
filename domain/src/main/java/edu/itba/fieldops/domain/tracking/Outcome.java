package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.shared.Texts;

public record Outcome(String text) {
    public Outcome {
        text = Texts.required(text, "result");
    }
}
