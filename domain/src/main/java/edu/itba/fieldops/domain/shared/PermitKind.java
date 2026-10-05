package edu.itba.fieldops.domain.shared;

public record PermitKind(String name) {
    public static final PermitKind ZONE = new PermitKind("zone");
    public static final PermitKind NIGHT = new PermitKind("night operation");

    public PermitKind {
        name = Texts.required(name, "permit kind");
    }
}
