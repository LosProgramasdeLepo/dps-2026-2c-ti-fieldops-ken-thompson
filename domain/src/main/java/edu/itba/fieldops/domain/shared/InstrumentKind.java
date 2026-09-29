package edu.itba.fieldops.domain.shared;

public record InstrumentKind(String name) {
    public InstrumentKind {
        name = Texts.required(name, "instrument kind");
    }
}
