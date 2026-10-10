package edu.itba.fieldops.adapters.jpa.expedition;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record StoredKind(
        @Column(name = "activity_id", nullable = false) UUID activityId,
        @Column(name = "kind", nullable = false) String kind
) {
}
