package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.ActivityId;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public final class ActivityBlock implements ItineraryItem {
    public enum Arrangement {
        SEQUENTIAL {
            @Override
            Duration combine(Stream<Duration> durations) {
                return durations.reduce(Duration.ZERO, Duration::plus);
            }
        },
        PARALLEL {
            @Override
            Duration combine(Stream<Duration> durations) {
                return durations.max(Comparator.naturalOrder()).orElseThrow();
            }
        };

        abstract Duration combine(Stream<Duration> durations);
    }

    private final Arrangement arrangement;
    private final List<ItineraryItem> parts;

    private ActivityBlock(Arrangement arrangement, List<ItineraryItem> parts) {
        if (parts.size() < 2) {
            throw new InvalidItinerary("block must group at least two parts");
        }
        for (ItineraryItem part : parts) {
            Objects.requireNonNull(part, "block part");
        }
        this.arrangement = arrangement;
        this.parts = List.copyOf(parts);
        requireDistinctActivities();
    }

    public static ActivityBlock sequential(ItineraryItem first, ItineraryItem second, ItineraryItem... rest) {
        return new ActivityBlock(Arrangement.SEQUENTIAL, parts(first, second, rest));
    }

    public static ActivityBlock parallel(ItineraryItem first, ItineraryItem second, ItineraryItem... rest) {
        return new ActivityBlock(Arrangement.PARALLEL, parts(first, second, rest));
    }

    public Arrangement arrangement() {
        return arrangement;
    }

    public List<ItineraryItem> parts() {
        return parts;
    }

    @Override
    public Duration duration(Function<Activity, Duration> leafDuration) {
        return arrangement.combine(parts.stream().map(part -> part.duration(leafDuration)));
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
        return new ActivityBlock(arrangement, parts);
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
