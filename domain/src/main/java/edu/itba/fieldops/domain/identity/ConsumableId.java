package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record ConsumableId(UUID value) {
    public ConsumableId {
        Objects.requireNonNull(value, "consumable id");
    }
}
