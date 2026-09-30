package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.ActivityId;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ActivityBlock implements ItineraryItem {
    private final boolean parallel;
    private final List<ItineraryItem> parts;

    private ActivityBlock(boolean parallel, List<ItineraryItem> parts) {
        if (parts == null || parts.size() < 2) {
            throw new InvalidItinerary("block must group at least two parts");
        }
        for (ItineraryItem part : parts) {
            Objects.requireNonNull(part, "block part");
        }
        this.parallel = parallel;
        this.parts = List.copyOf(parts);
        requireDistinctActivities();
    }

    public static ActivityBlock sequential(ItineraryItem first, ItineraryItem second, ItineraryItem... rest) {
        return new ActivityBlock(false, parts(first, second, rest));
    }

    public static ActivityBlock parallel(ItineraryItem first, ItineraryItem second, ItineraryItem... rest) {
        return new ActivityBlock(true, parts(first, second, rest));
    }

    public boolean parallel() {
        return parallel;
    }

    public List<ItineraryItem> parts() {
        return parts;
    }

    @Override
    public Duration estimatedDuration() {
        if (parallel) {
            return parts.stream()
                    .map(ItineraryItem::estimatedDuration)
                    .max(Comparator.naturalOrder())
                    .orElseThrow();
        }
        return parts.stream()
                .map(ItineraryItem::estimatedDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    @Override
    public List<Activity> activities() {
        List<Activity> leaves = new ArrayList<>();
        for (ItineraryItem part : parts) {
            leaves.addAll(part.activities());
        }
        return List.copyOf(leaves);
    }

    ActivityBlock withParts(List<ItineraryItem> parts) {
        return new ActivityBlock(parallel, parts);
    }

    private void requireDistinctActivities() {
        Set<ActivityId> seen = new HashSet<>();
        for (Activity activity : activities()) {
            if (!seen.add(activity.id())) {
                throw new InvalidItinerary("duplicate activity: " + activity.id());
            }
        }
    }

    private static List<ItineraryItem> parts(ItineraryItem first, ItineraryItem second, ItineraryItem[] rest) {
        List<ItineraryItem> parts = new ArrayList<>();
        parts.add(first);
        parts.add(second);
        parts.addAll(List.of(rest));
        return parts;
    }
}
