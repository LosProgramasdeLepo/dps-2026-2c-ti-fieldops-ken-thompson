package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record PersonId(UUID value) implements BookableId {
    public PersonId {
        Objects.requireNonNull(value, "person id");
    }

    @Override
    public String label() {
        return "person " + value;
    }
}
