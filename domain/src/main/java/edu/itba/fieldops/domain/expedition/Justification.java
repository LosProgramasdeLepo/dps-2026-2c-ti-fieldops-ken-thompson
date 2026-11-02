package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.Texts;

public record Justification(String text) {
    public Justification {
        text = Texts.required(text, "justification");
    }
}
