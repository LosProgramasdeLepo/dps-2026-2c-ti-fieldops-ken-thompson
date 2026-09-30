package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.VehicleRequirement;
import edu.itba.fieldops.domain.shared.InstrumentKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class AssignmentSuggester {
    public List<Assignment> suggest(PlanningContext context) {
        Suggestion suggestion = new Suggestion(context);
        for (Activity activity : context.plan().activities()) {
            suggestion.fillGaps(activity);
        }
        return suggestion.suggested();
    }

    private static final class Suggestion {
        private final BookableResources resources;
        private final Assignments current;
        private final List<TemporalBooking> taken;
        private final List<Assignment> suggested = new ArrayList<>();

        private Suggestion(PlanningContext context) {
            this.resources = context.bookable();
            this.current = context.plan().assignments();
            this.taken = new ArrayList<>(TemporalBooking.of(context.plan()));
            this.taken.addAll(context.occupying().bookings());
        }

        private void fillGaps(Activity activity) {
            fillVehicle(activity);
            fillInstrument(activity);
            fillPeople(activity);
        }

        private List<Assignment> suggested() {
            return List.copyOf(suggested);
        }

        private void fillVehicle(Activity activity) {
            if (activity.requirements().vehicle() == VehicleRequirement.NONE || !current.vehiclesOf(activity.id()).isEmpty()) {
                return;
            }
            takeFirstFree(
                    resources.vehicles().vehicles().stream().map(vehicle -> new VehicleAssignment(activity.id(), vehicle.id())),
                    activity
            );
        }

        private void fillInstrument(Activity activity) {
            Optional<InstrumentKind> kind = activity.requirements().instrument().requiredKind();
            if (kind.isEmpty() || !current.instrumentsOf(activity.id()).isEmpty()) {
                return;
            }
            takeFirstFree(
                    resources.instruments().instruments().stream()
                            .filter(instrument -> instrument.kind().equals(kind.get()))
                            .map(instrument -> new InstrumentAssignment(activity.id(), instrument.id())),
                    activity
            );
        }

        private void fillPeople(Activity activity) {
            for (CertificationId certification : activity.requirements().certifications()) {
                if (!heldByAssignee(activity, certification)) {
                    takeFirstFree(peopleHolding(activity, person -> person.holds(certification)), activity);
                }
            }
        }

        private Stream<PersonAssignment> peopleHolding(Activity activity, Predicate<Person> eligible) {
            return resources.people().people().stream()
                    .filter(eligible)
                    .map(person -> new PersonAssignment(activity.id(), person.id()));
        }

        private boolean heldByAssignee(Activity activity, CertificationId certification) {
            return current.peopleOf(activity.id()).stream()
                    .map(assignment -> resources.people().person(assignment.personId()))
                    .flatMap(Optional::stream)
                    .anyMatch(person -> person.holds(certification));
        }

        private void takeFirstFree(Stream<? extends BookableAssignment> candidates, Activity activity) {
            candidates.filter(candidate -> isFree(candidate.booking(activity.window())))
                    .findFirst()
                    .ifPresent(candidate -> take(candidate, activity));
        }

        private boolean isFree(TemporalBooking booking) {
            return booking.availableIn(resources) && taken.stream().noneMatch(booking::conflicts);
        }

        private void take(BookableAssignment assignment, Activity activity) {
            suggested.add(assignment);
            current.add(assignment);
            taken.add(assignment.booking(activity.window()));
        }
    }
}
