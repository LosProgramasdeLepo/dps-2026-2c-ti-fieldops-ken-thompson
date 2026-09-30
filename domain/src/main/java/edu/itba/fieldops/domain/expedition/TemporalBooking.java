package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.List;
import java.util.Objects;

public record TemporalBooking(BookableAssignment assignment, TimePeriod window) {
    public TemporalBooking {
        Objects.requireNonNull(assignment, "assignment");
        Objects.requireNonNull(window, "window");
    }

    public static List<TemporalBooking> of(Expedition expedition) {
        return expedition.assignments().bookable().stream()
                .map(assignment -> assignment.booking(expedition.activityOf(assignment.activityId()).window()))
                .toList();
    }

    public ActivityId activityId() {
        return assignment.activityId();
    }

    public String label() {
        return assignment.resourceId().label();
    }

    public boolean availableIn(BookableResources resources) {
        return assignment.resourceIn(resources).filter(resource -> resource.availableDuring(window)).isPresent();
    }

    public boolean unavailableIn(BookableResources resources) {
        return assignment.resourceIn(resources).filter(resource -> !resource.availableDuring(window)).isPresent();
    }

    public boolean conflicts(TemporalBooking other) {
        return assignment.resourceId().equals(other.assignment.resourceId()) && window.overlaps(other.window);
    }
}
