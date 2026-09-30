package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Itinerary {
    private final List<ItineraryItem> items = new ArrayList<>();

    public void add(ItineraryItem item) {
        Objects.requireNonNull(item, "itinerary item");
        List<Activity> incoming = item.activities();
        List<Activity> current = activities();
        List<Activity> universe = new ArrayList<>(current);
        universe.addAll(incoming);
        for (Activity activity : incoming) {
            if (current.stream().anyMatch(existing -> existing.id().equals(activity.id()))) {
                throw new InvalidItinerary("duplicate activity: " + activity.id());
            }
            requireKnownPredecessors(activity, universe);
            requireAcyclic(activity, universe);
            requirePredecessorsFinishBefore(activity, universe);
        }
        items.add(item);
    }

    public void remove(ActivityId activityId) {
        activityOf(activityId);
        for (Activity activity : activities()) {
            if (activity.predecessors().contains(activityId)) {
                replace(activity.withoutPredecessor(activityId));
            }
        }
        List<ItineraryItem> next = new ArrayList<>();
        for (ItineraryItem item : items) {
            without(item, activityId).ifPresent(next::add);
        }
        items.clear();
        items.addAll(next);
    }

    public void addDependency(ActivityId activityId, ActivityId predecessorId) {
        Objects.requireNonNull(predecessorId, "predecessor id");
        Activity activity = activityOf(activityId);
        Activity predecessor = activityOf(predecessorId);
        if (activity.predecessors().contains(predecessor.id())) {
            throw new InvalidItinerary("duplicate predecessor: " + predecessorId);
        }
        Activity updated = activity.withPredecessor(predecessor.id());
        List<Activity> universe = withActivity(activities(), updated);
        requireAcyclic(updated, universe);
        requirePredecessorsFinishBefore(updated, universe);
        replace(updated);
    }

    public void delay(ActivityId activityId, Duration delay) {
        for (Activity activity : delayed(activityId, delay)) {
            replace(activity);
        }
    }

    public List<Activity> delayed(ActivityId activityId, Duration delay) {
        Objects.requireNonNull(delay, "delay");
        List<Activity> next = new ArrayList<>(activities());
        Activity target = in(next, activityId);
        next.set(indexIn(next, activityId), target.withWindow(target.window().shifted(delay)));
        boolean moved;
        do {
            moved = false;
            for (int index = 0; index < next.size(); index++) {
                Activity activity = next.get(index);
                Instant ready = readyToStart(activity, next);
                if (activity.window().start().isBefore(ready)) {
                    Duration length = Duration.between(activity.window().start(), activity.window().end());
                    next.set(index, activity.withWindow(new TimePeriod(ready, ready.plus(length))));
                    moved = true;
                }
            }
        } while (moved);
        return List.copyOf(next);
    }

    public Activity activityOf(ActivityId activityId) {
        return in(activities(), activityId);
    }

    public List<Activity> activities() {
        List<Activity> leaves = new ArrayList<>();
        for (ItineraryItem item : items) {
            leaves.addAll(item.activities());
        }
        return List.copyOf(leaves);
    }

    public List<ItineraryItem> items() {
        return List.copyOf(items);
    }

    public Duration estimatedDuration() {
        return items.stream()
                .map(ItineraryItem::estimatedDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    public RiskLevel risk() {
        return activities().stream()
                .map(Activity::risk)
                .max(Comparator.naturalOrder())
                .orElse(RiskLevel.LOW);
    }

    public Map<ConsumableId, Stock> estimatedConsumption() {
        Map<ConsumableId, Stock> estimated = new HashMap<>();
        for (Activity activity : activities()) {
            activity.requirements().estimatedConsumption()
                    .forEach((id, quantity) -> estimated.merge(id, quantity, Stock::plus));
        }
        return Map.copyOf(estimated);
    }

    public Itinerary copy() {
        Itinerary copy = new Itinerary();
        for (ItineraryItem item : items) {
            copy.items.add(copyOf(item));
        }
        return copy;
    }

    private static void requireKnownPredecessors(Activity activity, List<Activity> universe) {
        for (ActivityId predecessorId : activity.predecessors()) {
            in(universe, predecessorId);
        }
    }

    private static void requireAcyclic(Activity activity, List<Activity> universe) {
        if (reaches(activity.id(), activity.predecessors(), new HashSet<>(), universe)) {
            throw new InvalidItinerary("activity dependencies form a cycle");
        }
    }

    private static boolean reaches(ActivityId target, Set<ActivityId> from, Set<ActivityId> seen, List<Activity> universe) {
        for (ActivityId predecessorId : from) {
            if (predecessorId.equals(target)) {
                return true;
            }
            if (!seen.add(predecessorId)) {
                continue;
            }
            if (reaches(target, in(universe, predecessorId).predecessors(), seen, universe)) {
                return true;
            }
        }
        return false;
    }

    private static void requirePredecessorsFinishBefore(Activity activity, List<Activity> universe) {
        for (ActivityId predecessorId : activity.predecessors()) {
            Activity predecessor = in(universe, predecessorId);
            if (!predecessor.window().finishesBeforeStartOf(activity.window())) {
                throw new InvalidItinerary("predecessor must finish before activity starts: " + predecessorId);
            }
        }
    }

    private static Instant readyToStart(Activity activity, List<Activity> source) {
        Instant ready = activity.window().start();
        for (ActivityId predecessorId : activity.predecessors()) {
            Instant end = in(source, predecessorId).window().end();
            if (end.isAfter(ready)) {
                ready = end;
            }
        }
        return ready;
    }

    private static Activity in(List<Activity> source, ActivityId activityId) {
        return source.get(indexIn(source, activityId));
    }

    private void replace(Activity updated) {
        for (int index = 0; index < items.size(); index++) {
            items.set(index, replaceIn(items.get(index), updated));
        }
    }

    private static ItineraryItem replaceIn(ItineraryItem item, Activity updated) {
        return switch (item) {
            case Activity activity -> activity.id().equals(updated.id()) ? updated : activity;
            case ActivityBlock block -> {
                List<ItineraryItem> next = new ArrayList<>();
                for (ItineraryItem part : block.parts()) {
                    next.add(replaceIn(part, updated));
                }
                yield block.withParts(next);
            }
        };
    }

    private static Optional<ItineraryItem> without(ItineraryItem item, ActivityId activityId) {
        return switch (item) {
            case Activity activity -> activity.id().equals(activityId) ? Optional.empty() : Optional.of(activity);
            case ActivityBlock block -> {
                List<ItineraryItem> kept = new ArrayList<>();
                for (ItineraryItem part : block.parts()) {
                    without(part, activityId).ifPresent(kept::add);
                }
                if (kept.isEmpty()) {
                    yield Optional.empty();
                }
                if (kept.size() == 1) {
                    yield Optional.of(kept.getFirst());
                }
                yield Optional.of(block.withParts(kept));
            }
        };
    }

    private static ItineraryItem copyOf(ItineraryItem item) {
        return switch (item) {
            case Activity activity -> activity;
            case ActivityBlock block -> {
                List<ItineraryItem> copies = new ArrayList<>();
                for (ItineraryItem part : block.parts()) {
                    copies.add(copyOf(part));
                }
                yield block.withParts(copies);
            }
        };
    }

    private static List<Activity> withActivity(List<Activity> source, Activity updated) {
        List<Activity> next = new ArrayList<>(source);
        next.set(indexIn(next, updated.id()), updated);
        return next;
    }

    private static int indexIn(List<Activity> source, ActivityId activityId) {
        for (int index = 0; index < source.size(); index++) {
            if (source.get(index).id().equals(activityId)) {
                return index;
            }
        }
        throw new InvalidItinerary("unknown activity: " + activityId);
    }
}
