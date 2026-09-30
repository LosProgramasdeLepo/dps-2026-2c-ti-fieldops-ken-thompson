package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.details.ResourceCatalog;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentSuggesterTest {
    private final AssignmentSuggester suggester = new AssignmentSuggester();

    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final InstrumentKind PROBE = new InstrumentKind("probe");

    @Test
    void proposesCertifiedPersonForSampling() {
        Certification certification = certification();
        Person ada = person("Ada", certification, Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);

        List<Assignment> suggestions = suggester.suggest(contextOf(expedition, catalog));

        assertEquals(List.of(new PersonAssignment(activity.id(), ada.id())), suggestions);
    }

    @Test
    void doesNotProposeWhenRequirementsAreAlreadyAssigned() {
        Certification certification = certification();
        Person ada = person("Ada", certification, Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), ada.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);

        List<Assignment> suggestions = suggester.suggest(contextOf(expedition, catalog));

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void skipsPersonTakenByOccupyingOverlapAndPicksTheNext() {
        Certification certification = certification();
        Person ada = person("Ada", certification, Availability.always());
        Person bob = person("Bob", certification, Availability.always());
        Activity firstActivity = sampling(certification.id(), 0, 4);
        Expedition occupying = draftWith(firstActivity);
        occupying.addAssignment(new PersonAssignment(firstActivity.id(), ada.id()));
        occupying.submitForReview();
        Activity secondActivity = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(secondActivity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(bob);

        List<Assignment> suggestions = suggester.suggest(new PlanningContext(expedition, catalog.catalogs(), OccupyingExpeditions.of(expedition, List.of(occupying), Map.of())));

        assertEquals(List.of(new PersonAssignment(secondActivity.id(), bob.id())), suggestions);
    }

    @Test
    void reusesPersonOnAdjacentWindow() {
        Certification certification = certification();
        Person ada = person("Ada", certification, Availability.always());
        Activity morning = sampling(certification.id(), 0, 4);
        Expedition occupying = draftWith(morning);
        occupying.addAssignment(new PersonAssignment(morning.id(), ada.id()));
        occupying.submitForReview();
        Activity afternoon = sampling(certification.id(), 4, 8);
        Expedition expedition = draftWith(afternoon);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);

        List<Assignment> suggestions = suggester.suggest(new PlanningContext(expedition, catalog.catalogs(), OccupyingExpeditions.of(expedition, List.of(occupying), Map.of())));

        assertEquals(List.of(new PersonAssignment(afternoon.id(), ada.id())), suggestions);
    }

    @Test
    void proposesVehicleForTransit() {
        Vehicle vehicle = new Vehicle(new VehicleId(UUID.randomUUID()), new Passengers(4), Availability.always());
        Activity activity = transit(0, 2);
        Expedition expedition = draftWith(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(vehicle);

        List<Assignment> suggestions = suggester.suggest(contextOf(expedition, catalog));

        assertEquals(List.of(new VehicleAssignment(activity.id(), vehicle.id())), suggestions);
    }

    @Test
    void proposesInstrumentAndOperatorForMeasurement() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Operator");
        Person ada = person("Ada", certification, Availability.always());
        Instrument meter = new Instrument(new InstrumentId(UUID.randomUUID()), PROBE, Availability.always());
        Activity activity = measurement(certification.id(), 0, 3);
        Expedition expedition = draftWith(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(meter);

        List<Assignment> suggestions = suggester.suggest(contextOf(expedition, catalog));

        assertEquals(
                List.of(
                        new InstrumentAssignment(activity.id(), meter.id()),
                        new PersonAssignment(activity.id(), ada.id())
                ),
                suggestions
        );
    }

    @Test
    void doesNotProposeUnavailablePerson() {
        Certification certification = certification();
        Person ada = person("Ada", certification, new Availability(List.of(window(0, 4))));
        Activity activity = sampling(certification.id(), 4, 8);
        Expedition expedition = draftWith(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);

        List<Assignment> suggestions = suggester.suggest(contextOf(expedition, catalog));

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void applyingSuggestionsFillsSamplingRequirements() {
        Certification certification = certification();
        Person ada = person("Ada", certification, Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);

        suggester.suggest(contextOf(expedition, catalog)).forEach(expedition::addAssignment);

        assertEquals(List.of(new PersonAssignment(activity.id(), ada.id())), expedition.assignments().all());
    }

    private static PlanningContext contextOf(Expedition plan, ResourceCatalog catalog) {
        return new PlanningContext(plan, catalog.catalogs(), OccupyingExpeditions.none());
    }

    private static Certification certification() {
        return new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
    }

    private static Person person(String name, Certification certification, Availability availability) {
        return new Person(new PersonId(UUID.randomUUID()), name, List.of(certification), availability);
    }

    private static Expedition draftWith(Activity activity) {
        Expedition expedition = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland biodiversity")),
                        week(),
                        List.of(DELTA),
                        List.of(new PersonId(UUID.randomUUID())),
                        List.of(new Restriction("No night work"))
                )
        );
        expedition.addActivity(activity);
        return expedition;
    }

    private static Activity sampling(CertificationId certificationId, int fromHour, int toHour) {
        return Activity.sampling(certificationId)
                .named(new ActivityId(UUID.randomUUID()), "sample")
                .estimated(Duration.ofHours(toHour - fromHour), RiskLevel.MEDIUM)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static Activity transit(int fromHour, int toHour) {
        return Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "transit")
                .estimated(Duration.ofHours(toHour - fromHour), RiskLevel.LOW)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static Activity measurement(CertificationId certificationId, int fromHour, int toHour) {
        return Activity.measurement(certificationId, PROBE)
                .named(new ActivityId(UUID.randomUUID()), "measure")
                .estimated(Duration.ofHours(toHour - fromHour), RiskLevel.HIGH)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plusSeconds(fromHour * 3600L), DAY.plusSeconds(toHour * 3600L));
    }

    private static TimePeriod week() {
        return new TimePeriod(DAY, DAY.plusSeconds(86_400L * 5));
    }
}
