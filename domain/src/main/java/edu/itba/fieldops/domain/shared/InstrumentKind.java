package edu.itba.fieldops.domain.shared;

public record InstrumentKind(String name) {
    public static final InstrumentKind LIGHTING = new InstrumentKind("lighting");
    public static final InstrumentKind DIVING_GEAR = new InstrumentKind("diving gear");
    public static final InstrumentKind CAMP_GEAR = new InstrumentKind("camp gear");

    public InstrumentKind {
        name = Texts.required(name, "instrument kind");
    }
}
