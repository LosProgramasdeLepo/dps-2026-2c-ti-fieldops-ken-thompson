package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class Itinerary {
    private final List<Activity> activities = new ArrayList<>();

    public void add(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        requireUnknown(activity.id());
        requireKnownPredecessors(activity);
        requireAcyclic(activity);
        requirePredecessorsFinishBefore(activity);
        activities.add(activity);
    }

    public void remove(ActivityId activityId) {
        activityOf(activityId);
        for (int index = 0; index < activities.size(); index++) {
            Activity activity = activities.get(index);
            if (activity.predecessors().contains(activityId)) {
                activities.set(index, activity.withoutPredecessor(activityId));
            }
        }
        activities.removeIf(activity -> activity.id().equals(activityId));
    }

    public void addDependency(ActivityId activityId, ActivityId predecessorId) {
        Objects.requireNonNull(predecessorId, "predecessor id");
        Activity activity = activityOf(activityId);
        Activity predecessor = activityOf(predecessorId);
        if (activity.predecessors().contains(predecessor.id())) {
            throw new InvalidItinerary("duplicate predecessor: " + predecessorId);
        }
        Activity updated = activity.withPredecessor(predecessor.id());
        requireAcyclic(updated);
        requirePredecessorsFinishBefore(updated);
        replace(updated);
    }

    public void delay(ActivityId activityId, Duration delay) {
        List<Activity> next = delayed(activityId, delay);
        activities.clear();
        activities.addAll(next);
    }

    public List<Activity> delayed(ActivityId activityId, Duration delay) {
        Objects.requireNonNull(delay, "delay");
        List<Activity> next = new ArrayList<>(activities);
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
        return activities.get(indexOf(activityId));
    }

    public List<Activity> activities() {
        return List.copyOf(activities);
    }

    public Itinerary copy() {
        Itinerary copy = new Itinerary();
        copy.activities.addAll(activities);
        return copy;
    }

    private void requireUnknown(ActivityId activityId) {
        if (activities.stream().anyMatch(activity -> activity.id().equals(activityId))) {
            throw new InvalidItinerary("duplicate activity: " + activityId);
        }
    }

    private void requireKnownPredecessors(Activity activity) {
        activity.predecessors().forEach(this::activityOf);
    }

    private void requireAcyclic(Activity activity) {
        if (reaches(activity.id(), activity.predecessors(), new HashSet<>())) {
            throw new InvalidItinerary("activity dependencies form a cycle");
        }
    }

    private boolean reaches(ActivityId target, Set<ActivityId> from, Set<ActivityId> seen) {
        for (ActivityId predecessorId : from) {
            if (predecessorId.equals(target)) {
                return true;
            }
            if (!seen.add(predecessorId)) {
                continue;
            }
            if (reaches(target, activityOf(predecessorId).predecessors(), seen)) {
                return true;
            }
        }
        return false;
    }

    private void requirePredecessorsFinishBefore(Activity activity) {
        for (ActivityId predecessorId : activity.predecessors()) {
            Activity predecessor = activityOf(predecessorId);
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
        activities.set(indexOf(updated.id()), updated);
    }

    private int indexOf(ActivityId activityId) {
        return indexIn(activities, activityId);
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
