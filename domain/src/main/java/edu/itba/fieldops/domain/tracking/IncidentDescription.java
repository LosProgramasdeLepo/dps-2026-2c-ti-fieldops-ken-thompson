package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.shared.Texts;

public record IncidentDescription(String text) {
    public IncidentDescription {
        text = Texts.required(text, "incident");
    }
}
