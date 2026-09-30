package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.Incident;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Replanner {
    private final AssignmentSuggester suggester;

    public Replanner(AssignmentSuggester suggester) {
        this.suggester = Objects.requireNonNull(suggester, "assignment suggester");
    }

    void cancel(PlanningContext context, ActivityId activityId) {
        requireDraft(context.plan()).removeActivity(activityId);
        refill(context);
    }

    void delay(PlanningContext context, ActivityId activityId, Duration delay) {
        requireDraft(context.plan()).delay(activityId, delay);
        dropInvalid(context);
        refill(context);
    }

    void replaceUnavailable(PlanningContext context) {
        requireDraft(context.plan());
        dropInvalid(context);
        refill(context);
    }

    void respondTo(PlanningContext context, Incident incident, ExpeditionExecution execution) {
        ActivityId activityId = incident.activityId()
                .orElseThrow(() -> new InvalidValue("incident must affect an activity"));
        Activity activity = context.plan().activityOf(activityId);
        if (execution.hasStarted(activityId)) {
            cancel(context, activityId);
        } else if (incident.at().isAfter(activity.window().start())) {
            delayOrCancel(context, activityId, Duration.between(activity.window().start(), incident.at()));
        } else {
            replaceUnavailable(context);
        }
    }

    private void delayOrCancel(PlanningContext context, ActivityId activityId, Duration delay) {
        if (context.plan().delayFits(activityId, delay)) {
            delay(context, activityId, delay);
        } else {
            cancel(context, activityId);
        }
    }

    private static Expedition requireDraft(Expedition plan) {
        if (plan.status() != ExpeditionStatus.DRAFT) {
            throw new InvalidExpeditionTransition(plan.status(), "replan");
        }
        return plan;
    }

    private void refill(PlanningContext context) {
        for (Assignment assignment : suggester.suggest(context)) {
            context.plan().addAssignment(assignment);
        }
    }

    private static void dropInvalid(PlanningContext context) {
        for (BookableAssignment assignment : invalidAssignments(context)) {
            context.plan().removeAssignment(assignment);
        }
    }

    private static List<BookableAssignment> invalidAssignments(PlanningContext context) {
        Expedition plan = context.plan();
        List<TemporalBooking> occupied = context.occupying().bookings();
        List<TemporalBooking> kept = new ArrayList<>();
        List<BookableAssignment> invalid = new ArrayList<>();
        for (BookableAssignment assignment : plan.assignments().bookable()) {
            TemporalBooking booking = assignment.booking(plan.activityOf(assignment.activityId()).window());
            boolean usable = booking.availableIn(context.bookable())
                    && occupied.stream().noneMatch(booking::conflicts)
                    && kept.stream().noneMatch(booking::conflicts);
            if (usable) {
                kept.add(booking);
            } else {
                invalid.add(assignment);
            }
        }
        return invalid;
    }
}
