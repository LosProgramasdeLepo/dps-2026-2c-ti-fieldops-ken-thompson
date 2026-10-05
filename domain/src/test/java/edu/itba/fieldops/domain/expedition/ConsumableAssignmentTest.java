package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Stock;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsumableAssignmentTest {
    @Test
    void rejectsAConsumableAssignmentOfNothing() {
        ActivityId activityId = new ActivityId(UUID.randomUUID());
        ConsumableId vials = new ConsumableId(UUID.randomUUID());

        assertThrows(InvalidAssignment.class, () -> new ConsumableAssignment(activityId, vials, new Stock(0)));
    }
}
