package edu.itba.fieldops.domain.shared;

public record InstrumentKind(String name) {
    public static final InstrumentKind LIGHTING = new InstrumentKind("lighting");

    public InstrumentKind {
        name = Texts.required(name, "instrument kind");
    }
}
