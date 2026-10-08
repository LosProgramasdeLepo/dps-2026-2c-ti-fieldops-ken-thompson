package edu.itba.fieldops.adapters.jpa.expedition;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Optional;
import java.util.UUID;

@Embeddable
public record StoredNode(
        @Column(name = "parent") Integer parent,
        @Column(name = "arrangement") String arrangement,
        @Column(name = "activity_id") UUID activityId
) {
    static StoredNode block(Optional<Integer> parent, String arrangement) {
        return new StoredNode(parent.orElse(null), arrangement, null);
    }

    static StoredNode leaf(Optional<Integer> parent, UUID activityId) {
        return new StoredNode(parent.orElse(null), null, activityId);
    }

    Optional<Integer> enclosingBlock() {
        return Optional.ofNullable(parent);
    }

    Optional<UUID> activity() {
        return Optional.ofNullable(activityId);
    }
}
