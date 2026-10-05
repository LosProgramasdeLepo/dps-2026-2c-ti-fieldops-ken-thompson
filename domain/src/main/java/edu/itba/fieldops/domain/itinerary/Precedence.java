package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.ActivityId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Precedence {
    private Precedence() {
    }

    static Map<ActivityId, Set<ActivityId>> of(List<ItineraryItem> items) {
        Map<ActivityId, Set<ActivityId>> merged = new HashMap<>();
        for (ItineraryItem item : items) {
            merge(merged, item.precedence());
        }
        return merged;
    }

    static void merge(Map<ActivityId, Set<ActivityId>> into, Map<ActivityId, Set<ActivityId>> other) {
        other.forEach((activityId, predecessors) -> into.computeIfAbsent(activityId, id -> new HashSet<>()).addAll(predecessors));
    }
}
