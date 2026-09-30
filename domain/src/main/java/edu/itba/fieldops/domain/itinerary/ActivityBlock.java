package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.ActivityId;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ActivityBlock implements ItineraryItem {
    public enum Arrangement {
        SEQUENTIAL {
            @Override
            Duration combine(Stream<Duration> durations) {
                return durations.reduce(Duration.ZERO, Duration::plus);
            }

            @Override
            Map<ActivityId, Set<ActivityId>> order(List<ItineraryItem> parts) {
                Map<ActivityId, Set<ActivityId>> order = new HashMap<>();
                for (int index = 1; index < parts.size(); index++) {
                    Set<ActivityId> before = ids(parts.get(index - 1));
                    for (ActivityId after : ids(parts.get(index))) {
                        order.put(after, before);
                    }
                }
                return order;
            }

            @Override
            List<ItineraryItem> concurrent(List<ItineraryItem> parts) {
                return List.of();
            }
        },
        PARALLEL {
            @Override
            Duration combine(Stream<Duration> durations) {
                return durations.max(Comparator.naturalOrder()).orElseThrow();
            }

            @Override
            Map<ActivityId, Set<ActivityId>> order(List<ItineraryItem> parts) {
                return Map.of();
            }

            @Override
            List<ItineraryItem> concurrent(List<ItineraryItem> parts) {
                return parts;
            }
        };

        abstract Duration combine(Stream<Duration> durations);

        abstract Map<ActivityId, Set<ActivityId>> order(List<ItineraryItem> parts);

        abstract List<ItineraryItem> concurrent(List<ItineraryItem> parts);
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

    public List<ItineraryItem> parts() {
        return parts;
    }

    public List<ItineraryItem> concurrentParts() {
        return arrangement.concurrent(parts);
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

    @Override
    public List<ActivityBlock> blocks() {
        List<ActivityBlock> blocks = new ArrayList<>();
        blocks.add(this);
        for (ItineraryItem part : parts) {
            blocks.addAll(part.blocks());
        }
        return List.copyOf(blocks);
    }

    @Override
    public Map<ActivityId, Set<ActivityId>> precedence() {
        Map<ActivityId, Set<ActivityId>> precedence = Precedence.of(parts);
        Precedence.merge(precedence, arrangement.order(parts));
        return precedence;
    }

    @Override
    public ActivityBlock replacing(Activity updated) {
        return new ActivityBlock(arrangement, parts.stream().map(part -> part.replacing(updated)).toList());
    }

    @Override
    public Optional<ItineraryItem> without(ActivityId activityId) {
        List<ItineraryItem> kept = parts.stream().flatMap(part -> part.without(activityId).stream()).toList();
        if (kept.isEmpty()) {
            return Optional.empty();
        }
        if (kept.size() == 1) {
            return Optional.of(kept.getFirst());
        }
        return Optional.of(new ActivityBlock(arrangement, kept));
    }

    private static Set<ActivityId> ids(ItineraryItem item) {
        return item.activities().stream().map(Activity::id).collect(Collectors.toSet());
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
