package edu.itba.fieldops.domain.shared;

public final class UnknownResource extends DomainException {
    private final String id;

    public UnknownResource(String resource, String id) {
        super("unknown " + Texts.required(resource, "resource") + ": " + Texts.required(id, "id"));
        this.id = Texts.required(id, "id");
    }

    public String id() {
        return id;
    }
}
