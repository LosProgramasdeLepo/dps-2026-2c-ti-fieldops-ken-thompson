package edu.itba.fieldops.adapters.jpa.expedition;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record StoredRequirement(
        @Column(name = "activity_id", nullable = false) UUID activityId,
        @Column(name = "certification_id", nullable = false) UUID certificationId,
        @Column(name = "scope", nullable = false) String scope
) {
    static final String SOMEONE = "SOMEONE";
    static final String EVERYONE = "EVERYONE";
}
