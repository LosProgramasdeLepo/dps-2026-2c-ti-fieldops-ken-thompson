package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.itinerary.Itinerary;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

final class PlanContent {
    private final ExpeditionCharter charter;
    private final Itinerary itinerary;
    private final Assignments assignments;
    private final List<PermitId> permits;

    PlanContent(ExpeditionCharter charter) {
        this(Objects.requireNonNull(charter, "charter"), new Itinerary(), new Assignments(), new ArrayList<>());
    }

    private PlanContent(ExpeditionCharter charter, Itinerary itinerary, Assignments assignments, List<PermitId> permits) {
        this.charter = charter;
        this.itinerary = itinerary;
        this.assignments = assignments;
        this.permits = permits;
    }

    PlanContent copy() {
        return new PlanContent(charter, itinerary.copy(), assignments.copy(), new ArrayList<>(permits));
    }

    void addActivity(Activity activity) {
        requirePlanned(Objects.requireNonNull(activity, "activity"));
        itinerary.add(activity);
    }

    void addBlock(ActivityBlock block) {
        Objects.requireNonNull(block, "block").activities().forEach(this::requirePlanned);
        itinerary.add(block);
    }

    void removeActivity(ActivityId activityId) {
        itinerary.remove(activityId);
        assignments.removeActivity(activityId);
    }

    void addDependency(ActivityId activityId, ActivityId predecessorId) {
        itinerary.addDependency(activityId, predecessorId);
    }

    void delay(ActivityId activityId, Duration delay) {
        if (Objects.requireNonNull(delay, "delay").isNegative()) {
            throw new InvalidValue("delay must not be negative");
        }
        if (!delayFits(activityId, delay)) {
            throw new InvalidItinerary("activity window is outside the expedition period");
        }
        itinerary.delay(activityId, delay);
    }

    boolean delayFits(ActivityId activityId, Duration delay) {
        return itinerary.delayed(activityId, delay).stream()
                .allMatch(activity -> charter.period().contains(activity.window()));
    }

    void addAssignment(Assignment assignment) {
        itinerary.activityOf(Objects.requireNonNull(assignment, "assignment").activityId());
        assignments.add(assignment);
    }

    void removeAssignment(Assignment assignment) {
        assignments.remove(Objects.requireNonNull(assignment, "assignment"));
    }

    void addPermit(PermitId permitId) {
        if (permits.contains(Objects.requireNonNull(permitId, "permit id"))) {
            throw new InvalidValue("duplicate permit: " + permitId);
        }
        permits.add(permitId);
    }

    ExpeditionCharter charter() {
        return charter;
    }

    List<Activity> activities() {
        return itinerary.activities();
    }

    List<ItineraryItem> items() {
        return itinerary.items();
    }

    List<ActivityBlock> blocks() {
        return itinerary.blocks();
    }

    Activity activityOf(ActivityId activityId) {
        return itinerary.activityOf(activityId);
    }

    Set<ActivityId> predecessorsOf(ActivityId activityId) {
        return itinerary.predecessorsOf(activityId);
    }

    Duration duration(Function<Activity, Duration> leafDuration) {
        return itinerary.duration(leafDuration);
    }

    Duration estimatedDuration() {
        return itinerary.estimatedDuration();
    }

    RiskLevel estimatedRisk() {
        return itinerary.risk();
    }

    Map<ConsumableId, Stock> estimatedConsumption() {
        return itinerary.estimatedConsumption();
    }

    Assignments assignments() {
        return assignments.copy();
    }

    List<PermitId> permits() {
        return List.copyOf(permits);
    }

    private void requirePlanned(Activity activity) {
        if (!charter.zones().contains(activity.zone())) {
            throw new InvalidItinerary("activity zone is not part of the expedition");
        }
        if (!charter.period().contains(activity.window())) {
            throw new InvalidItinerary("activity window is outside the expedition period");
        }
    }
}
