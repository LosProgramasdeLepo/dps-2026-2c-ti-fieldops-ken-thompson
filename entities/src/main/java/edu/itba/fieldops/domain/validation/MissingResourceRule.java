package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Instruments;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.Assignments;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.PlanningContext;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.InstrumentKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MissingResourceRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(PlanningContext context) {
        Assignments assignments = context.plan().assignments();
        List<ValidationIssue> issues = new ArrayList<>();
        for (Assignment assignment : assignments.all()) {
            assignment.unknownIn(context.catalogs()).ifPresent(label -> issues.add(critical("unknown " + label)));
        }
        for (Activity activity : context.plan().activities()) {
            missingVehicle(activity, assignments).ifPresent(issues::add);
            issues.addAll(missingInstruments(activity, assignments, context.instruments()));
            missingPersonnel(activity, assignments).ifPresent(issues::add);
        }
        return issues;
    }

    private static Optional<ValidationIssue> missingVehicle(Activity activity, Assignments assignments) {
        int required = activity.requirements().vehicles();
        if (assignments.vehiclesOf(activity.id()).size() < required) {
            return Optional.of(critical("activity " + activity.name() + " requires " + required + " vehicles and has fewer assigned"));
        }
        return Optional.empty();
    }

    private static List<ValidationIssue> missingInstruments(Activity activity, Assignments assignments, Instruments instruments) {
        return activity.requirements().instruments().stream()
                .filter(kind -> lacksInstrumentOf(kind, assignments.instrumentsOf(activity.id()), instruments))
                .map(kind -> critical(
                        "activity " + activity.name() + " requires an instrument of kind " + kind.name() + " and has none assigned"
                ))
                .toList();
    }

    private static Optional<ValidationIssue> missingPersonnel(Activity activity, Assignments assignments) {
        if (activity.requirements().needsCertifiedPersonnel() && assignments.peopleOf(activity.id()).isEmpty()) {
            return Optional.of(critical("activity " + activity.name() + " requires certified personnel and has none assigned"));
        }
        return Optional.empty();
    }

    private static boolean lacksInstrumentOf(InstrumentKind kind, List<InstrumentAssignment> assigned, Instruments instruments) {
        if (assigned.isEmpty()) {
            return true;
        }
        List<Instrument> known = assigned.stream()
                .map(assignment -> instruments.instrument(assignment.instrumentId()))
                .flatMap(Optional::stream)
                .toList();
        return !known.isEmpty() && known.stream().noneMatch(instrument -> instrument.kind().equals(kind));
    }

    private static ValidationIssue critical(String message) {
        return new ValidationIssue(IssueSeverity.CRITICAL, "RESOURCE", message);
    }
}
