package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class Itinerary {
    private final List<ItineraryItem> items = new ArrayList<>();

    public void add(ItineraryItem item) {
        Objects.requireNonNull(item, "itinerary item");
        List<Activity> current = activities();
        for (Activity incoming : item.activities()) {
            if (current.stream().anyMatch(existing -> existing.id().equals(incoming.id()))) {
                throw new InvalidItinerary("duplicate activity: " + incoming.id());
            }
        }
        List<ItineraryItem> next = new ArrayList<>(items);
        next.add(item);
        requireConsistent(item.activities(), next);
        items.add(item);
    }

    public void remove(ActivityId activityId) {
        activityOf(activityId);
        for (Activity activity : activities()) {
            if (activity.predecessors().contains(activityId)) {
                replace(activity.withoutPredecessor(activityId));
            }
        }
        List<ItineraryItem> next = items.stream().flatMap(item -> item.without(activityId).stream()).toList();
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
        requireConsistent(List.of(updated), replaced(items, updated));
        replace(updated);
    }

    public void delay(ActivityId activityId, Duration delay) {
        for (Activity activity : delayed(activityId, delay)) {
            replace(activity);
        }
    }

    public List<Activity> delayed(ActivityId activityId, Duration delay) {
        Objects.requireNonNull(delay, "delay");
        List<Activity> schedule = new ArrayList<>(activities());
        int index = indexOf(schedule, activityId);
        Activity target = schedule.get(index);
        schedule.set(index, target.withWindow(target.window().shifted(delay)));
        pushDependents(schedule, Precedence.of(items));
        return List.copyOf(schedule);
    }

    public Activity activityOf(ActivityId activityId) {
        return find(activities(), activityId);
    }

    public Set<ActivityId> predecessorsOf(ActivityId activityId) {
        activityOf(activityId);
        return Set.copyOf(Precedence.of(items).get(activityId));
    }

    public List<Activity> activities() {
        return leaves(items);
    }

    public List<ItineraryItem> items() {
        return List.copyOf(items);
    }

    public List<ActivityBlock> blocks() {
        return items.stream().flatMap(item -> item.blocks().stream()).toList();
    }

    public Duration duration(Function<Activity, Duration> leafDuration) {
        return items.stream()
                .map(item -> item.duration(leafDuration))
                .reduce(Duration.ZERO, Duration::plus);
    }

    public Duration estimatedDuration() {
        return duration(Activity::estimatedDuration);
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
            activity.estimatedConsumption().forEach((id, quantity) -> estimated.merge(id, quantity, Stock::plus));
        }
        return Map.copyOf(estimated);
    }

    public Itinerary copy() {
        Itinerary copy = new Itinerary();
        copy.items.addAll(items);
        return copy;
    }

    private void replace(Activity updated) {
        List<ItineraryItem> next = replaced(items, updated);
        items.clear();
        items.addAll(next);
    }

    private static void requireConsistent(List<Activity> changed, List<ItineraryItem> tree) {
        List<Activity> universe = leaves(tree);
        Map<ActivityId, Set<ActivityId>> precedence = Precedence.of(tree);
        for (Activity activity : changed) {
            requireKnown(precedence.get(activity.id()), universe);
        }
        for (Activity activity : changed) {
            requireAcyclic(activity.id(), precedence);
            requirePredecessorsFinishBefore(activity, precedence.get(activity.id()), universe);
        }
    }

    private static void requireKnown(Set<ActivityId> predecessors, List<Activity> universe) {
        for (ActivityId predecessorId : predecessors) {
            find(universe, predecessorId);
        }
    }

    private static void requireAcyclic(ActivityId activityId, Map<ActivityId, Set<ActivityId>> precedence) {
        if (reaches(activityId, precedence.get(activityId), precedence)) {
            throw new InvalidItinerary("activity dependencies form a cycle");
        }
    }

    private static boolean reaches(ActivityId target, Set<ActivityId> from, Map<ActivityId, Set<ActivityId>> precedence) {
        Set<ActivityId> seen = new HashSet<>();
        Deque<ActivityId> pending = new ArrayDeque<>(from);
        while (!pending.isEmpty()) {
            ActivityId next = pending.pop();
            if (next.equals(target)) {
                return true;
            }
            if (seen.add(next)) {
                pending.addAll(precedence.get(next));
            }
        }
        return false;
    }

    private static void requirePredecessorsFinishBefore(Activity activity, Set<ActivityId> predecessors, List<Activity> universe) {
        for (ActivityId predecessorId : predecessors) {
            if (!find(universe, predecessorId).window().finishesBeforeStartOf(activity.window())) {
                throw new InvalidItinerary("predecessor must finish before activity starts: " + predecessorId);
            }
        }
    }

    private static void pushDependents(List<Activity> schedule, Map<ActivityId, Set<ActivityId>> precedence) {
        boolean moved;
        do {
            moved = false;
            for (int index = 0; index < schedule.size(); index++) {
                Activity activity = schedule.get(index);
                Instant ready = readyToStart(activity, precedence.get(activity.id()), schedule);
                if (activity.window().start().isBefore(ready)) {
                    Duration push = Duration.between(activity.window().start(), ready);
                    schedule.set(index, activity.withWindow(activity.window().shifted(push)));
                    moved = true;
                }
            }
        } while (moved);
    }

    private static Instant readyToStart(Activity activity, Set<ActivityId> predecessors, List<Activity> schedule) {
        Instant ready = activity.window().start();
        for (ActivityId predecessorId : predecessors) {
            Instant end = find(schedule, predecessorId).window().end();
            if (end.isAfter(ready)) {
                ready = end;
            }
        }
        return ready;
    }

    private static List<Activity> leaves(List<ItineraryItem> tree) {
        List<Activity> leaves = new ArrayList<>();
        for (ItineraryItem item : tree) {
            leaves.addAll(item.activities());
        }
        return List.copyOf(leaves);
    }

    private static List<ItineraryItem> replaced(List<ItineraryItem> tree, Activity updated) {
        return tree.stream().map(item -> item.replacing(updated)).toList();
    }

    private static Activity find(List<Activity> source, ActivityId activityId) {
        return source.get(indexOf(source, activityId));
    }

    private static int indexOf(List<Activity> source, ActivityId activityId) {
        for (int index = 0; index < source.size(); index++) {
            if (source.get(index).id().equals(activityId)) {
                return index;
            }
        }
        throw new InvalidItinerary("unknown activity: " + activityId);
    }
}
