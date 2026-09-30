package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.VehicleRequirement;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AssignmentSuggester {
    public List<Assignment> suggest(Expedition expedition, Catalog catalog, OccupyingExpeditions peers) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(peers, "peers");
        List<TemporalBooking> taken = new ArrayList<>(TemporalBooking.of(expedition));
        for (Expedition peer : peers.plans()) {
            taken.addAll(TemporalBooking.of(peer));
        }
        Assignments current = expedition.assignments().copy();
        List<Assignment> suggestions = new ArrayList<>();
        for (Activity activity : expedition.itinerary()) {
            fillGaps(activity, catalog, taken, suggestions, current);
        }
        return List.copyOf(suggestions);
    }

    private static void fillGaps(
            Activity activity,
            Catalog catalog,
            List<TemporalBooking> taken,
            List<Assignment> suggestions,
            Assignments current
    ) {
        TimePeriod window = activity.window();
        if (activity.requirements().vehicle() == VehicleRequirement.REQUIRED && current.vehiclesOf(activity.id()).isEmpty()) {
            catalog.vehicles().stream()
                    .filter(vehicle -> vehicle.availableDuring(window))
                    .filter(vehicle -> free(taken, new TemporalBooking.VehicleBooking(vehicle.id(), activity.id(), window)))
                    .findFirst()
                    .ifPresent(vehicle -> take(
                            suggestions,
                            current,
                            taken,
                            new VehicleAssignment(activity.id(), vehicle.id()),
                            window
                    ));
        }
        Optional<InstrumentKind> requiredKind = activity.requirements().instrument().requiredKind();
        if (requiredKind.isPresent() && current.instrumentsOf(activity.id()).isEmpty()) {
            catalog.instruments().stream()
                    .filter(instrument -> instrument.kind().equals(requiredKind.get()))
                    .filter(instrument -> instrument.availableDuring(window))
                    .filter(instrument -> free(taken, new TemporalBooking.InstrumentBooking(instrument.id(), activity.id(), window)))
                    .findFirst()
                    .ifPresent(instrument -> take(
                            suggestions,
                            current,
                            taken,
                            new InstrumentAssignment(activity.id(), instrument.id()),
                            window
                    ));
        }
        for (CertificationId certificationId : activity.requirements().certifications()) {
            if (heldBy(current, activity, catalog, certificationId)) {
                continue;
            }
            catalog.people().stream()
                    .filter(person -> person.holds(certificationId))
                    .filter(person -> person.availableDuring(window))
                    .map(Person::id)
                    .filter(id -> free(taken, new TemporalBooking.PersonBooking(id, activity.id(), window)))
                    .findFirst()
                    .ifPresent(personId -> take(
                            suggestions,
                            current,
                            taken,
                            new PersonAssignment(activity.id(), personId),
                            window
                    ));
        }
    }

    private static void take(
            List<Assignment> suggestions,
            Assignments current,
            List<TemporalBooking> taken,
            Assignment assignment,
            TimePeriod window
    ) {
        suggestions.add(assignment);
        current.add(assignment);
        assignment.booking(window).ifPresent(taken::add);
    }

    private static boolean heldBy(Assignments current, Activity activity, Catalog catalog, CertificationId certificationId) {
        for (PersonAssignment person : current.peopleOf(activity.id())) {
            if (catalog.person(person.personId()).filter(found -> found.holds(certificationId)).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean free(List<TemporalBooking> taken, TemporalBooking candidate) {
        return taken.stream().noneMatch(candidate::conflicts);
    }
}
