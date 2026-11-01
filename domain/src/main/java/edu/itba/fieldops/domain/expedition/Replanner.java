package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.Delay;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.Incident;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Replanner {
    private final AssignmentSuggester suggester;
    private final PlanningContexts contexts;

    public Replanner(AssignmentSuggester suggester, PlanningContexts contexts) {
        this.suggester = Objects.requireNonNull(suggester, "assignment suggester");
        this.contexts = Objects.requireNonNull(contexts, "planning contexts");
    }

    public void cancel(Expedition draft, ActivityId activityId) {
        cancel(contextOf(draft), activityId);
    }

    public void delay(Expedition draft, ActivityId activityId, Delay delay) {
        delay(contextOf(draft), activityId, delay);
    }

    public void replaceUnavailable(Expedition draft) {
        replaceUnavailable(contextOf(draft));
    }

    ReplanProposal propose(ProposalId id, Expedition revision, Incident incident, ExpeditionExecution execution) {
        if (!execution.incidents().contains(incident)) {
            throw new InvalidValue("incident is not recorded on the run");
        }
        ReplanProposal proposal = new ReplanProposal(id, incident, revision);
        respondTo(contextOf(revision), incident, execution);
        return proposal;
    }

    private PlanningContext contextOf(Expedition draft) {
        return contexts.around(requireDraft(draft));
    }

    private void cancel(PlanningContext context, ActivityId activityId) {
        context.plan().removeActivity(activityId);
        refill(context);
    }

    private void delay(PlanningContext context, ActivityId activityId, Delay delay) {
        context.plan().delay(activityId, delay);
        dropInvalid(context);
        refill(context);
    }

    private void replaceUnavailable(PlanningContext context) {
        dropInvalid(context);
        refill(context);
    }

    private void respondTo(PlanningContext context, Incident incident, ExpeditionExecution execution) {
        ActivityId activityId = incident.activityId()
                .orElseThrow(() -> new InvalidValue("incident must affect an activity"));
        Activity activity = context.plan().activityOf(activityId);
        if (execution.hasStarted(activityId)) {
            cancel(context, activityId);
        } else if (incident.at().isAfter(activity.window().start())) {
            delayOrCancel(context, activityId, new Delay(Duration.between(activity.window().start(), incident.at())));
        } else {
            replaceUnavailable(context);
        }
    }

    private void delayOrCancel(PlanningContext context, ActivityId activityId, Delay delay) {
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
            context.plan().addAssignment(assignment, context.catalogs());
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
