package edu.itba.fieldops.adapters.jpa.tracking;

import edu.itba.fieldops.domain.tracking.ActivityExecution;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.List;
import java.util.UUID;

@Embeddable
public record StoredActivityConsumption(
        @Column(name = "activity_id", nullable = false) UUID activityId,
        @Column(name = "consumable_id", nullable = false) UUID consumableId,
        @Column(name = "quantity", nullable = false) int quantity
) {
    static List<StoredActivityConsumption> of(ActivityExecution execution) {
        return execution.consumed().entrySet().stream()
                .map(entry -> new StoredActivityConsumption(
                        execution.activityId().value(),
                        entry.getKey().value(),
                        entry.getValue().amount()
                ))
                .toList();
    }
}
