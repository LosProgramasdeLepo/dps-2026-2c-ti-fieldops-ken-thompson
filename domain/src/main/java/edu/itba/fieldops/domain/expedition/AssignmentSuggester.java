package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.InstrumentRequirement;
import edu.itba.fieldops.domain.itinerary.VehicleRequirement;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AssignmentSuggester {
    public List<Assignment> suggest(Expedition expedition, Catalog catalog, OccupyingExpeditions peers) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(peers, "peers");
        List<TemporalBooking> taken = new ArrayList<>(TemporalBooking.of(expedition));
        for (Expedition peer : peers.plans()) {
            taken.addAll(TemporalBooking.of(peer));
        }
        List<Assignment> suggestions = new ArrayList<>();
        for (Activity activity : expedition.itinerary()) {
            fillGaps(activity, expedition, catalog, taken, suggestions);
        }
        return List.copyOf(suggestions);
    }

    private static void fillGaps(
            Activity activity,
            Expedition expedition,
            Catalog catalog,
            List<TemporalBooking> taken,
            List<Assignment> suggestions
    ) {
        TimePeriod window = activity.window();
        List<Assignment> current = new ArrayList<>(expedition.assignments().of(activity.id()));
        if (activity.requirements().vehicle() == VehicleRequirement.REQUIRED && none(current, VehicleAssignment.class)) {
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
        if (activity.requirements().instrument() instanceof InstrumentRequirement.OfKind required && none(current, InstrumentAssignment.class)) {
            catalog.instruments().stream()
                    .filter(instrument -> instrument.kind().equals(required.kind()))
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
            if (heldBy(current, catalog, certificationId)) {
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
            List<Assignment> current,
            List<TemporalBooking> taken,
            Assignment assignment,
            TimePeriod window
    ) {
        suggestions.add(assignment);
        current.add(assignment);
        assignment.booking(window).ifPresent(taken::add);
    }

    private static boolean heldBy(List<Assignment> current, Catalog catalog, CertificationId certificationId) {
        for (PersonAssignment person : people(current)) {
            if (catalog.person(person.personId()).filter(found -> found.holds(certificationId)).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static List<PersonAssignment> people(List<Assignment> current) {
        List<PersonAssignment> people = new ArrayList<>();
        for (Assignment assignment : current) {
            if (assignment instanceof PersonAssignment person) {
                people.add(person);
            }
        }
        return people;
    }

    private static boolean none(List<Assignment> current, Class<? extends Assignment> type) {
        return current.stream().noneMatch(type::isInstance);
    }

    private static boolean free(List<TemporalBooking> taken, TemporalBooking candidate) {
        return taken.stream().noneMatch(candidate::conflicts);
    }
}
