package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record PersonId(UUID value) {
    public PersonId {
        Objects.requireNonNull(value, "person id");
    }
}
