package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.ConsumableName;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.catalog.Stock;
import edu.itba.fieldops.domain.event.ActivityCompleted;
import edu.itba.fieldops.domain.event.ActivityStarted;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.RunFinished;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.Incidents;
import edu.itba.fieldops.domain.support.ResourceCatalog;
import edu.itba.fieldops.domain.support.StoredExpeditions;
import edu.itba.fieldops.domain.support.StoredPeople;
import edu.itba.fieldops.domain.support.StoredPlans;
import edu.itba.fieldops.domain.support.StoredRuns;
import edu.itba.fieldops.domain.tracking.ActivityReport;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.IncidentReport;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.domain.tracking.ObservationText;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static edu.itba.fieldops.domain.support.Submissions.submit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionTrackerTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    private final StoredRuns runs = new StoredRuns();
    private final ResourceCatalog catalog = new ResourceCatalog();
    private Instant now = DAY;

    @Test
    void aPlanDoesNotStartASecondRun() {
        Expedition plan = approved();
        ExpeditionTracker tracker = trackerOf(plan);
        runs.save(tracker.start(plan.id()));

        InvalidExpeditionTransition rejected = assertThrows(
                InvalidExpeditionTransition.class,
                () -> tracker.start(plan.id())
        );

        assertEquals("cannot start while expedition is IN_PROGRESS", rejected.getMessage());
    }

    @Test
    void aRevisionDoesNotStartAnotherRunOfTheLineage() {
        Expedition original = approved();
        Expedition revision = original.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
        submit(revision);
        Expedition approvedRevision = StoredPlans.approved(revision);
        ExpeditionTracker tracker = trackerOf(original, approvedRevision);
        runs.save(tracker.start(original.id()));

        InvalidExpeditionTransition rejected = assertThrows(
                InvalidExpeditionTransition.class,
                () -> tracker.start(approvedRevision.id())
        );

        assertEquals("cannot start while expedition is IN_PROGRESS", rejected.getMessage());
    }

    @Test
    void anActivityDoesNotFinishAfterThePeriod() {
        Expedition plan = approved();
        ActivityId activity = plan.activities().getFirst().id();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        tracker.startActivity(run, activity);
        now = DAY.plus(Duration.ofDays(6));

        InvalidActivityExecution rejected = assertThrows(
                InvalidActivityExecution.class,
                () -> tracker.finishActivity(run, activity, ActivityReport.of(new Outcome("samples stored")))
        );

        assertEquals("finish is outside the expedition period", rejected.getMessage());
        assertFalse(run.activities().getFirst().isFinished());
    }

    @Test
    void anActivityRecordsWhatItConsumed() {
        Consumable fuel = new Consumable(new ConsumableId(UUID.randomUUID()), new ConsumableName("fuel"), new Stock(50));
        catalog.save(fuel);
        Expedition plan = approved();
        ActivityId activity = plan.activities().getFirst().id();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        tracker.startActivity(run, activity);
        now = DAY.plus(Duration.ofHours(2));

        tracker.finishActivity(run, activity, new ActivityReport(new Outcome("site reached"), Map.of(fuel.id(), new Consumption(8))));

        assertAll(
                () -> assertEquals(Map.of(fuel.id(), new Consumption(8)), run.activities().getFirst().consumed()),
                () -> assertEquals(Optional.of(DAY.plus(Duration.ofHours(2))), run.activities().getFirst().finishedAt())
        );
    }

    @Test
    void anActivityDoesNotRecordAConsumableTheCatalogDoesNotKnow() {
        Expedition plan = approved();
        ActivityId activity = plan.activities().getFirst().id();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        tracker.startActivity(run, activity);
        ConsumableId unknown = new ConsumableId(UUID.randomUUID());
        ActivityReport report = new ActivityReport(new Outcome("site reached"), Map.of(unknown, new Consumption(1)));

        UnknownResource rejected = assertThrows(UnknownResource.class, () -> tracker.finishActivity(run, activity, report));

        assertEquals("unknown consumable: " + unknown.value(), rejected.getMessage());
        assertFalse(run.activities().getFirst().isFinished());
    }

    @Test
    void anIncidentIsRecordedAtTheClockOnAnActivityOfThePlan() {
        Expedition plan = approved();
        ActivityId activity = plan.activities().getFirst().id();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        now = DAY.plus(Duration.ofHours(1));

        IncidentId id = Incidents.newId();

        Incident incident = tracker.reportIncident(run, IncidentReport.affecting(id, activity, new IncidentDescription("flooded trail")));

        assertAll(
                () -> assertEquals(
                        new Incident(id, new IncidentDescription("flooded trail"), DAY.plus(Duration.ofHours(1)), Optional.of(activity)),
                        incident
                ),
                () -> assertEquals(List.of(incident), run.incidents())
        );
    }

    @Test
    void anIncidentDoesNotNameAnActivityOutsideThePlan() {
        Expedition plan = approved();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        ActivityId stranger = new ActivityId(UUID.randomUUID());

        InvalidActivityExecution rejected = assertThrows(
                InvalidActivityExecution.class,
                () -> tracker.reportIncident(run, IncidentReport.affecting(Incidents.newId(), stranger, new IncidentDescription("flooded trail")))
        );

        assertEquals("unknown activity: " + stranger, rejected.getMessage());
        assertTrue(run.incidents().isEmpty());
    }

    @Test
    void anIncidentIsNotRecordedAfterThePeriod() {
        Expedition plan = approved();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        now = DAY.plus(Duration.ofDays(6));

        InvalidActivityExecution rejected = assertThrows(
                InvalidActivityExecution.class,
                () -> tracker.reportIncident(run, IncidentReport.general(Incidents.newId(), new IncidentDescription("storm")))
        );

        assertEquals("incident is outside the expedition period", rejected.getMessage());
        assertTrue(run.incidents().isEmpty());
    }

    @Test
    void anObservationIsRecordedAtTheClockWithinThePeriod() {
        Expedition plan = approved();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        now = DAY.plus(Duration.ofHours(3));

        tracker.observe(run, new ObservationText("heron colony"));
        now = DAY.plus(Duration.ofDays(6));

        assertAll(
                () -> assertEquals(List.of(new Observation(new ObservationText("heron colony"), DAY.plus(Duration.ofHours(3)))), run.observations()),
                () -> assertThrows(InvalidActivityExecution.class, () -> tracker.observe(run, new ObservationText("late note")))
        );
    }

    @Test
    void startingARunRegistersRunStarted() {
        Expedition plan = approved();

        ExpeditionExecution run = trackerOf(plan).start(plan.id());

        assertEquals(List.of(new RunStarted(plan.id(), DAY)), run.pullEvents());
    }

    @Test
    void startingAnActivityRegistersActivityStarted() {
        Expedition plan = approved();
        ActivityId activity = plan.activities().getFirst().id();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        run.pullEvents();

        tracker.startActivity(run, activity);

        assertEquals(List.of(new ActivityStarted(plan.id(), DAY, activity)), run.pullEvents());
    }

    @Test
    void finishingAnActivityRegistersTheResultStartAndConsumption() {
        Consumable fuel = new Consumable(new ConsumableId(UUID.randomUUID()), new ConsumableName("fuel"), new Stock(50));
        catalog.save(fuel);
        Expedition plan = approved();
        ActivityId activity = plan.activities().getFirst().id();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        tracker.startActivity(run, activity);
        run.pullEvents();
        now = DAY.plus(Duration.ofHours(2));

        tracker.finishActivity(run, activity, new ActivityReport(new Outcome("site reached"), Map.of(fuel.id(), new Consumption(8))));

        assertEquals(
                List.of(new ActivityCompleted(
                        plan.id(), now, activity, DAY, new Outcome("site reached"), Map.of(fuel.id(), new Consumption(8))
                )),
                run.pullEvents()
        );
    }

    @Test
    void anUnknownConsumableRegistersNothing() {
        Expedition plan = approved();
        ActivityId activity = plan.activities().getFirst().id();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        tracker.startActivity(run, activity);
        run.pullEvents();
        ActivityReport report = new ActivityReport(new Outcome("done"), Map.of(new ConsumableId(UUID.randomUUID()), new Consumption(1)));

        assertThrows(UnknownResource.class, () -> tracker.finishActivity(run, activity, report));

        assertEquals(List.of(), run.pullEvents());
    }

    @Test
    void reportingAnIncidentRegistersIncidentReportedWithItsId() {
        Expedition plan = approved();
        ActivityId activity = plan.activities().getFirst().id();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        run.pullEvents();
        IncidentId general = Incidents.newId();
        IncidentId affecting = Incidents.newId();

        tracker.reportIncident(run, IncidentReport.general(general, new IncidentDescription("storm")));
        tracker.reportIncident(run, IncidentReport.affecting(affecting, activity, new IncidentDescription("flood")));

        assertEquals(
                List.of(
                        new IncidentReported(plan.id(), DAY, general, Optional.empty(), new IncidentDescription("storm")),
                        new IncidentReported(plan.id(), DAY, affecting, Optional.of(activity), new IncidentDescription("flood"))
                ),
                run.pullEvents()
        );
    }

    @Test
    void anIncidentThatFailsRegistersNothing() {
        Expedition plan = approved();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        run.pullEvents();
        ActivityId stranger = new ActivityId(UUID.randomUUID());
        now = DAY.plus(Duration.ofDays(6));

        assertThrows(InvalidActivityExecution.class, () -> tracker.reportIncident(run, IncidentReport.general(Incidents.newId(), new IncidentDescription("late"))));
        now = DAY;
        assertThrows(InvalidActivityExecution.class, () -> tracker.reportIncident(run, IncidentReport.affecting(Incidents.newId(), stranger, new IncidentDescription("elsewhere"))));

        assertEquals(List.of(), run.pullEvents());
    }

    @Test
    void aRepeatedIncidentIdIsRejected() {
        Expedition plan = approved();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        IncidentId id = Incidents.newId();
        tracker.reportIncident(run, IncidentReport.general(id, new IncidentDescription("storm")));
        run.pullEvents();

        assertThrows(InvalidValue.class, () -> tracker.reportIncident(run, IncidentReport.general(id, new IncidentDescription("again"))));

        assertAll(
                () -> assertEquals(1, run.incidents().size()),
                () -> assertEquals(List.of(), run.pullEvents())
        );
    }

    @Test
    void finishingTheRunRegistersRunFinished() {
        Expedition plan = approved();
        ActivityId activity = plan.activities().getFirst().id();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        tracker.startActivity(run, activity);
        tracker.finishActivity(run, activity, ActivityReport.of(new Outcome("done")));
        run.pullEvents();
        now = DAY.plus(Duration.ofHours(3));

        tracker.finish(run);

        assertEquals(List.of(new RunFinished(plan.id(), now)), run.pullEvents());
    }

    @Test
    void aRestoredRunHasNoPendingEvents() {
        Expedition plan = approved();
        ExpeditionExecution run = trackerOf(plan).start(plan.id());

        assertEquals(List.of(), ExpeditionExecution.restore(run.state()).pullEvents());
    }

    @Test
    void suspendingResumingAndObservingRegisterNoEvents() {
        Expedition plan = approved();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        run.pullEvents();

        run.suspend();
        run.resume();
        tracker.observe(run, new ObservationText("heron colony"));

        assertEquals(List.of(), run.pullEvents());
    }

    private ExpeditionTracker trackerOf(Expedition... plans) {
        return new ExpeditionTracker(new StoredExpeditions(plans), runs, catalog, () -> now);
    }

    private static Expedition approved() {
        PersonId lead = new PersonId(UUID.randomUUID());
        Expedition plan = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                        List.of(DELTA),
                        List.of(lead),
                        List.of(new Restriction("Daylight only"))
                ),
                StoredPeople.of(new Person(lead, new PersonName("Ada"), List.of(), Availability.always()))
        );
        plan.addActivity(Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), new ActivityName("Approach"))
                .estimated(new EstimatedDuration(Duration.ofHours(2)), RiskLevel.LOW)
                .in(DELTA, new TimePeriod(DAY, DAY.plus(Duration.ofHours(4))))
                .build());
        submit(plan);
        return StoredPlans.approved(plan);
    }
}
