package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.VehicleRequirement;
import edu.itba.fieldops.domain.shared.InstrumentKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MissingResourceRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(ValidationContext context) {
        Expedition expedition = context.expedition();
        Catalog catalog = context.catalog();
        List<ValidationIssue> issues = new ArrayList<>();
        for (Assignment assignment : expedition.assignments().all()) {
            unknown(catalog, assignment).ifPresent(issues::add);
        }
        for (Activity activity : expedition.itinerary()) {
            issues.addAll(missingRequired(expedition, catalog, activity));
        }
        return issues;
    }

    private static Optional<ValidationIssue> unknown(Catalog catalog, Assignment assignment) {
        return assignment.unknownIn(catalog).map(label -> critical("unknown " + label));
    }

    private static List<ValidationIssue> missingRequired(Expedition expedition, Catalog catalog, Activity activity) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (activity.requirements().vehicle() == VehicleRequirement.REQUIRED
                && expedition.assignments().vehiclesOf(activity.id()).isEmpty()) {
            issues.add(critical("activity " + activity.name() + " requires a vehicle and has none assigned"));
        }
        activity.requirements().instrument().requiredKind().ifPresent(kind -> {
            List<InstrumentAssignment> assigned = expedition.assignments().instrumentsOf(activity.id());
            if (assigned.isEmpty() || knownInstrumentMissesKind(catalog, assigned, kind)) {
                issues.add(critical(
                        "activity " + activity.name() + " requires an instrument of kind " + kind.name() + " and has none assigned"
                ));
            }
        });
        if (!activity.requirements().certifications().isEmpty()
                && expedition.assignments().peopleOf(activity.id()).isEmpty()) {
            issues.add(critical("activity " + activity.name() + " requires certified personnel and has none assigned"));
        }
        return issues;
    }

    private static boolean knownInstrumentMissesKind(Catalog catalog, List<InstrumentAssignment> assigned, InstrumentKind kind) {
        boolean known = false;
        for (InstrumentAssignment assignment : assigned) {
            Optional<Instrument> instrument = catalog.instrument(assignment.instrumentId());
            if (instrument.isEmpty()) {
                continue;
            }
            known = true;
            if (instrument.get().kind().equals(kind)) {
                return false;
            }
        }
        return known;
    }

    private static ValidationIssue critical(String message) {
        return new ValidationIssue(IssueSeverity.CRITICAL, "RESOURCE", message);
    }
}
