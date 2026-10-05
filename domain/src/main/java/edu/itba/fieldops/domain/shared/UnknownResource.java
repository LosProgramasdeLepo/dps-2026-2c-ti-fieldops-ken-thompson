package edu.itba.fieldops.domain.shared;

public final class UnknownResource extends DomainException {
    private final String resource;
    private final String id;

    public UnknownResource(String resource, String id) {
        super("unknown " + Texts.required(resource, "resource") + ": " + Texts.required(id, "id"));
        this.resource = Texts.required(resource, "resource");
        this.id = Texts.required(id, "id");
    }

    public String resource() {
        return resource;
    }

    public String id() {
        return id;
    }
}
