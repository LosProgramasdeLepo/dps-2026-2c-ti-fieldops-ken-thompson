package edu.itba.fieldops.adapters.jpa.expedition;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record StoredConsumption(
        @Column(name = "activity_id", nullable = false) UUID activityId,
        @Column(name = "consumable_id", nullable = false) UUID consumableId,
        @Column(name = "quantity", nullable = false) int quantity
) {
}
