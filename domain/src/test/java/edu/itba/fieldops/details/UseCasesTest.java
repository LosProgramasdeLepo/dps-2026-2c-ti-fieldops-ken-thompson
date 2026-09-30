package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.AdministerCatalogInteractor;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.usecase.AdministerCatalog;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.ApproveExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.AssignResourcesInteractor;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.AssignmentSuggester;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.DraftExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.OccupyingExpeditions;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.PlanItineraryInteractor;
import edu.itba.fieldops.domain.expedition.ReplanExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.Replanner;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.expedition.ReviewExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.TrackExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.usecase.ApproveExpedition;
import edu.itba.fieldops.domain.expedition.usecase.AssignResources;
import edu.itba.fieldops.domain.expedition.usecase.DraftExpedition;
import edu.itba.fieldops.domain.expedition.usecase.PlanItinerary;
import edu.itba.fieldops.domain.expedition.usecase.ReplanExpedition;
import edu.itba.fieldops.domain.expedition.usecase.ReviewExpedition;
import edu.itba.fieldops.domain.expedition.usecase.TrackExpedition;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.report.Estimate;
import edu.itba.fieldops.domain.report.EstimateExpeditionInteractor;
import edu.itba.fieldops.domain.report.OperationalReport;
import edu.itba.fieldops.domain.report.OperationalStatus;
import edu.itba.fieldops.domain.report.ReportExpeditionInteractor;
import edu.itba.fieldops.domain.report.usecase.EstimateExpedition;
import edu.itba.fieldops.domain.report.usecase.ReportExpedition;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.domain.validation.ExpeditionValidator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseCasesTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final TimePeriod PERIOD = new TimePeriod(DAY, DAY.plus(Duration.ofDays(5)));

    private final ResourceCatalog catalog = new ResourceCatalog();
    private final InMemoryExpeditionRepository plans = new InMemoryExpeditionRepository();
    private final InMemoryExecutionRepository runs = new InMemoryExecutionRepository();
    private final FixedClock clock = new FixedClock(DAY);
    private final ExpeditionValidator validator = ExpeditionValidator.withDefaultRules();
    private final AdministerCatalog registry = new AdministerCatalogInteractor(catalog);
    private final DraftExpedition drafts = new DraftExpeditionInteractor(plans, catalog);
    private final PlanItinerary itinerary = new PlanItineraryInteractor(plans);
    private final EstimateExpedition estimates = new EstimateExpeditionInteractor(plans);
    private final AssignResources assignments = new AssignResourcesInteractor(plans, runs, catalog.catalogs(), new AssignmentSuggester());
    private final ReviewExpedition review = new ReviewExpeditionInteractor(plans, runs, catalog.catalogs(), validator);
    private final ApproveExpedition approval = new ApproveExpeditionInteractor(plans, runs, catalog.catalogs(), validator);
    private final TrackExpedition tracking = new TrackExpeditionInteractor(plans, runs, clock);
    private final ReplanExpedition replan = new ReplanExpeditionInteractor(
            plans,
            runs,
            catalog.bookable(),
            new Replanner(new AssignmentSuggester())
    );
    private final ReportExpedition reports = new ReportExpeditionInteractor(plans, runs);

    @Test
    void registersEachCatalogResourceAndKeepsCertificationOnThePerson() {
        Certification sampling = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        PersonId personId = registry.registerPerson("Ada", List.of(sampling), Availability.always());
        VehicleId vehicleId = registry.registerVehicle(new Passengers(4), Availability.always());
        InstrumentId instrumentId = registry.registerInstrument(new InstrumentKind("probe"), Availability.always());
        ConsumableId vials = registry.registerConsumable("vials", new Stock(20));
        PermitId permitId = registry.registerPermit(DELTA, PERIOD);

        assertTrue(catalog.person(personId).orElseThrow().holds(sampling.id()));
        assertTrue(catalog.vehicle(vehicleId).isPresent());
        assertTrue(catalog.instrument(instrumentId).isPresent());
        assertTrue(catalog.consumable(vials).isPresent());
        assertTrue(catalog.permit(permitId).isPresent());
    }

    @Test
    void rejectsAResponsibleThatIsNotInTheCatalog() {
        assertThrows(InvalidValue.class, () -> drafts.draft(
                List.of(new Objective("Map wetland")),
                PERIOD,
                List.of(DELTA),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("Daylight only"))
        ));
    }

    @Test
    void plansANightActivityAndTheEstimateUsesItsRaisedRisk() {
        CertificationId nightOperation = new CertificationId(UUID.randomUUID());
        PersonId ada = registry.registerPerson("Ada", List.of(new Certification(nightOperation, "Night operation")), Availability.always());
        InstrumentId lamp = registry.registerInstrument(new InstrumentKind("lighting"), Availability.always());
        PermitId permitId = registry.registerNightPermit(DELTA, PERIOD);
        ExpeditionId expeditionId = drafts.draft(
                List.of(new Objective("Night survey")),
                PERIOD,
                List.of(DELTA),
                List.of(ada),
                List.of(new Restriction("Stay on the water"))
        );
        ActivityId activityId = new ActivityId(UUID.randomUUID());
        itinerary.addActivity(expeditionId, Activity.night(
                activityId,
                "Night survey",
                Duration.ofHours(3),
                RiskLevel.MEDIUM,
                new TimePeriod(DAY, DAY.plus(Duration.ofHours(3))),
                Set.of(),
                DELTA,
                nightOperation,
                new InstrumentKind("lighting")
        ));
        assignments.addAssignment(expeditionId, new PersonAssignment(activityId, ada));
        assignments.addAssignment(expeditionId, new InstrumentAssignment(activityId, lamp));
        assignments.addPermit(expeditionId, permitId);

        Estimate estimate = estimates.of(expeditionId);
        OperationalReport report = reports.of(expeditionId);

        assertEquals(RiskLevel.HIGH, estimate.risk());
        assertEquals(Duration.ofHours(3), estimate.duration());
        assertEquals(RiskLevel.HIGH, report.risk());
        assertEquals(1, report.plannedActivities());
    }

    @Test
    void estimatesConsumptionFromRequirementsRatherThanAssignments() {
        Prepared prepared = samplingPlan(Map.of(new ConsumableId(UUID.randomUUID()), new Stock(3)));
        ConsumableId vials = registry.registerConsumable("vials", new Stock(20));
        assignments.addAssignment(prepared.expeditionId, new ConsumableAssignment(prepared.activityId, vials, new Stock(7)));

        Estimate estimate = estimates.of(prepared.expeditionId);

        assertEquals(Duration.ofHours(4), estimate.duration());
        assertEquals(RiskLevel.MEDIUM, estimate.risk());
        assertEquals(new Stock(3), estimate.estimatedConsumption().get(prepared.requiredConsumable));
        assertNull(estimate.estimatedConsumption().get(vials));
    }

    @Test
    void rejectsAnEmptyItineraryAndAWarningFromSomeoneWhoIsNotResponsible() {
        ExpeditionId expeditionId = draft();
        assertThrows(InvalidItinerary.class, () -> review.submit(expeditionId));

        Prepared prepared = samplingPlan(Map.of());
        review.submit(prepared.expeditionId);
        assertThrows(InvalidValue.class, () -> review.acceptWarning(
                prepared.expeditionId,
                new AcceptedWarning(
                        new ValidationIssue(IssueSeverity.WARNING, "CAPACITY", "vehicle near capacity"),
                        "backup team",
                        new PersonId(UUID.randomUUID())
                )
        ));
    }

    @Test
    void rejectsAnActivityStartOutsideItsWindow() {
        Prepared prepared = approvedSampling();
        clock.set(DAY.minus(Duration.ofHours(1)));

        assertThrows(InvalidActivityExecution.class, () -> tracking.startActivity(prepared.expeditionId, prepared.activityId));
        assertTrue(runs.find(prepared.expeditionId).orElseThrow().executions().isEmpty());
    }

    @Test
    void rejectsAnExpeditionStartOutsideThePeriod() {
        Prepared prepared = samplingPlan(Map.of());
        review.submit(prepared.expeditionId);
        approval.approve(prepared.expeditionId);
        clock.set(DAY.minus(Duration.ofHours(1)));

        assertThrows(InvalidActivityExecution.class, () -> tracking.start(prepared.expeditionId));
        assertTrue(runs.find(prepared.expeditionId).isEmpty());
    }

    @Test
    void replanWithdrawsAnApprovedPlanAndKeepsItOccupyingOnlyWhileTheRunContinues() {
        Prepared prepared = samplingPlan(Map.of());
        ActivityId ride = new ActivityId(UUID.randomUUID());
        CertificationId certificationId = new CertificationId(UUID.randomUUID());
        PersonId bob = registry.registerPerson("Bob", List.of(new Certification(certificationId, "Sampling")), Availability.always());
        itinerary.addActivity(prepared.expeditionId, Activity.sampling(
                ride,
                "Later sampling",
                Duration.ofHours(2),
                RiskLevel.LOW,
                new TimePeriod(DAY.plus(Duration.ofHours(4)), DAY.plus(Duration.ofHours(6))),
                Set.of(),
                DELTA,
                certificationId
        ));
        assignments.addAssignment(prepared.expeditionId, new PersonAssignment(ride, bob));
        review.submit(prepared.expeditionId);
        approval.approve(prepared.expeditionId);

        ExpeditionId revision = replan.cancel(prepared.expeditionId, ride);
        assertEquals(ExpeditionStatus.SUPERSEDED, plans.find(prepared.expeditionId).orElseThrow().status());
        assertTrue(occupying(revision).plans().isEmpty());
        assertEquals(ExpeditionStatus.DRAFT, plans.find(revision).orElseThrow().status());

        Prepared running = samplingPlan(Map.of());
        review.submit(running.expeditionId);
        approval.approve(running.expeditionId);
        tracking.start(running.expeditionId);
        tracking.startActivity(running.expeditionId, running.activityId);
        ExpeditionId next = replan.cancel(running.expeditionId, running.activityId);
        Prepared other = samplingPlan(Map.of());
        assertEquals(ExpeditionStatus.SUPERSEDED, plans.find(running.expeditionId).orElseThrow().status());
        assertTrue(occupying(next).plans().isEmpty());
        assertEquals(List.of(running.expeditionId), occupying(other.expeditionId).plans().stream().map(Expedition::id).toList());
    }

    @Test
    void cannotStartARevisionWhileTheOriginalRunContinues() {
        Prepared prepared = samplingPlan(Map.of());
        ActivityId later = new ActivityId(UUID.randomUUID());
        CertificationId certificationId = new CertificationId(UUID.randomUUID());
        PersonId bob = registry.registerPerson("Bob", List.of(new Certification(certificationId, "Sampling")), Availability.always());
        itinerary.addActivity(prepared.expeditionId, Activity.sampling(
                later,
                "Later sampling",
                Duration.ofHours(2),
                RiskLevel.LOW,
                new TimePeriod(DAY.plus(Duration.ofHours(4)), DAY.plus(Duration.ofHours(6))),
                Set.of(),
                DELTA,
                certificationId
        ));
        assignments.addAssignment(prepared.expeditionId, new PersonAssignment(later, bob));
        review.submit(prepared.expeditionId);
        approval.approve(prepared.expeditionId);
        tracking.start(prepared.expeditionId);
        tracking.startActivity(prepared.expeditionId, prepared.activityId);

        ExpeditionId revision = replan.cancel(prepared.expeditionId, prepared.activityId);
        review.submit(revision);
        approval.approve(revision);

        assertThrows(InvalidExpeditionTransition.class, () -> tracking.start(revision));
        assertTrue(runs.find(revision).isEmpty());
        assertEquals(ExpeditionExecution.Status.IN_PROGRESS, runs.find(prepared.expeditionId).orElseThrow().status());
    }

    @Test
    void finishFollowsTheRevisionItineraryAndStillClosesStartedWork() {
        Prepared prepared = samplingPlan(Map.of());
        ActivityId later = new ActivityId(UUID.randomUUID());
        CertificationId certificationId = new CertificationId(UUID.randomUUID());
        PersonId bob = registry.registerPerson("Bob", List.of(new Certification(certificationId, "Sampling")), Availability.always());
        itinerary.addActivity(prepared.expeditionId, Activity.sampling(
                later,
                "Later sampling",
                Duration.ofHours(2),
                RiskLevel.LOW,
                new TimePeriod(DAY.plus(Duration.ofHours(4)), DAY.plus(Duration.ofHours(6))),
                Set.of(),
                DELTA,
                certificationId
        ));
        assignments.addAssignment(prepared.expeditionId, new PersonAssignment(later, bob));
        review.submit(prepared.expeditionId);
        approval.approve(prepared.expeditionId);
        tracking.start(prepared.expeditionId);
        tracking.startActivity(prepared.expeditionId, prepared.activityId);
        replan.cancel(prepared.expeditionId, later);
        assertThrows(InvalidItinerary.class, () -> tracking.startActivity(prepared.expeditionId, later));
        clock.set(DAY.plus(Duration.ofHours(4)));
        tracking.finishActivity(prepared.expeditionId, prepared.activityId, "samples stored");

        tracking.finish(prepared.expeditionId);

        assertEquals(ExpeditionExecution.Status.FINISHED, runs.find(prepared.expeditionId).orElseThrow().status());
    }

    @Test
    void reportUsesActualDurationAndOnlyTheConsumptionOfFinishedActivities() {
        Prepared prepared = samplingPlan(Map.of());
        ActivityId second = new ActivityId(UUID.randomUUID());
        CertificationId certificationId = new CertificationId(UUID.randomUUID());
        PersonId bob = registry.registerPerson("Bob", List.of(new Certification(certificationId, "Sampling")), Availability.always());
        itinerary.addActivity(prepared.expeditionId, Activity.sampling(
                second,
                "Later sampling",
                Duration.ofHours(2),
                RiskLevel.LOW,
                new TimePeriod(DAY.plus(Duration.ofHours(4)), DAY.plus(Duration.ofHours(6))),
                Set.of(),
                DELTA,
                certificationId
        ));
        assignments.addAssignment(prepared.expeditionId, new PersonAssignment(second, bob));
        ConsumableId vials = registry.registerConsumable("vials", new Stock(20));
        assignments.addAssignment(prepared.expeditionId, new ConsumableAssignment(prepared.activityId, vials, new Stock(5)));
        assignments.addAssignment(prepared.expeditionId, new ConsumableAssignment(second, vials, new Stock(4)));
        review.submit(prepared.expeditionId);
        approval.approve(prepared.expeditionId);
        tracking.start(prepared.expeditionId);
        tracking.startActivity(prepared.expeditionId, prepared.activityId);
        clock.set(DAY.plus(Duration.ofHours(2)));
        tracking.finishActivity(prepared.expeditionId, prepared.activityId, "samples stored");

        OperationalReport report = reports.of(prepared.expeditionId);
        ActivityExecution run = runs.find(prepared.expeditionId).orElseThrow().executions().getFirst();

        assertEquals(DAY, run.startedAt());
        assertEquals(DAY.plus(Duration.ofHours(2)), run.finishedAt().orElseThrow());
        assertEquals(Duration.ofHours(2), report.duration());
        assertEquals(new Stock(5), report.consumption().get(vials));
        assertEquals(1, report.finishedActivities());
        assertEquals(OperationalStatus.IN_PROGRESS, report.status());
    }

    @Test
    void suggestsAssignmentsWithoutApplyingThem() {
        OpenSampling open = openSampling();

        List<Assignment> suggested = assignments.suggest(open.expeditionId);

        assertEquals(List.of(new PersonAssignment(open.activityId, open.personId)), suggested);
        assertTrue(plans.find(open.expeditionId).orElseThrow().assignments().all().isEmpty());
    }

    @Test
    void cannotSubmitWhileCriticalIssuesRemain() {
        OpenSampling open = openSampling();
        assertThrows(InvalidValue.class, () -> review.submit(open.expeditionId));
        assertEquals(ExpeditionStatus.DRAFT, plans.find(open.expeditionId).orElseThrow().status());
    }

    @Test
    void returnsToDraftFromReviewWithoutTouchingARun() {
        Prepared prepared = samplingPlan(Map.of());
        review.submit(prepared.expeditionId);

        review.returnToDraft(prepared.expeditionId);

        assertEquals(ExpeditionStatus.DRAFT, plans.find(prepared.expeditionId).orElseThrow().status());
        assertTrue(runs.find(prepared.expeditionId).isEmpty());
    }

    @Test
    void tracksSuspendResumeIncidentsObservationsAndFinish() {
        Prepared prepared = approvedSampling();
        tracking.startActivity(prepared.expeditionId, prepared.activityId);
        tracking.suspend(prepared.expeditionId);
        assertEquals(ExpeditionExecution.Status.SUSPENDED, runs.find(prepared.expeditionId).orElseThrow().status());

        tracking.resume(prepared.expeditionId);
        tracking.addIncident(prepared.expeditionId, "storm on site", prepared.activityId);
        tracking.addObservation(prepared.expeditionId, "ice on the trail");
        clock.set(DAY.plus(Duration.ofHours(4)));
        tracking.finishActivity(prepared.expeditionId, prepared.activityId, "samples stored");
        tracking.finish(prepared.expeditionId);

        OperationalReport report = reports.of(prepared.expeditionId);
        ExpeditionExecution execution = runs.find(prepared.expeditionId).orElseThrow();
        assertEquals(ExpeditionExecution.Status.FINISHED, execution.status());
        assertEquals(List.of(new Incident("storm on site", DAY, prepared.activityId)), execution.incidents());
        assertEquals(List.of(new Observation("ice on the trail", DAY)), execution.observations());
        assertEquals(OperationalStatus.FINISHED, report.status());
        assertEquals(Duration.ofHours(4), report.duration());
    }

    @Test
    void delayAndReplaceUnavailableGoThroughTheReplanUseCase() {
        Prepared prepared = samplingPlan(Map.of());
        review.submit(prepared.expeditionId);
        ExpeditionId delayed = replan.delay(prepared.expeditionId, prepared.activityId, Duration.ofHours(1));
        assertEquals(prepared.expeditionId, delayed);
        assertEquals(ExpeditionStatus.DRAFT, plans.find(delayed).orElseThrow().status());
        assertEquals(
                new TimePeriod(DAY.plus(Duration.ofHours(1)), DAY.plus(Duration.ofHours(5))),
                plans.find(delayed).orElseThrow().activityOf(prepared.activityId).window()
        );

        Prepared approved = samplingPlan(Map.of());
        review.submit(approved.expeditionId);
        approval.approve(approved.expeditionId);
        ExpeditionId revision = replan.replaceUnavailable(approved.expeditionId);
        assertEquals(ExpeditionStatus.SUPERSEDED, plans.find(approved.expeditionId).orElseThrow().status());
        assertEquals(ExpeditionStatus.DRAFT, plans.find(revision).orElseThrow().status());
        assertEquals(2, plans.find(revision).orElseThrow().version());
    }

    private OccupyingExpeditions occupying(ExpeditionId expeditionId) {
        Map<ExpeditionId, ExpeditionExecution> executions = new HashMap<>();
        for (Expedition plan : plans.all()) {
            runs.find(plan.id()).ifPresent(execution -> executions.put(plan.id(), execution));
        }
        return OccupyingExpeditions.of(plans.find(expeditionId).orElseThrow(), plans.all(), executions);
    }

    private Prepared approvedSampling() {
        Prepared prepared = samplingPlan(Map.of());
        review.submit(prepared.expeditionId);
        approval.approve(prepared.expeditionId);
        tracking.start(prepared.expeditionId);
        return prepared;
    }

    private Prepared samplingPlan(Map<ConsumableId, Stock> estimated) {
        CertificationId certificationId = new CertificationId(UUID.randomUUID());
        PersonId personId = registry.registerPerson(
                "Ada",
                List.of(new Certification(certificationId, "Sampling")),
                Availability.always()
        );
        PermitId permitId = registry.registerPermit(DELTA, PERIOD);
        ExpeditionId expeditionId = drafts.draft(
                List.of(new Objective("Map wetland")),
                PERIOD,
                List.of(DELTA),
                List.of(personId),
                List.of(new Restriction("Daylight only"))
        );
        ActivityId activityId = new ActivityId(UUID.randomUUID());
        itinerary.addActivity(expeditionId, Activity.sampling(
                activityId,
                "Soil sampling",
                Duration.ofHours(4),
                RiskLevel.MEDIUM,
                new TimePeriod(DAY, DAY.plus(Duration.ofHours(4))),
                Set.of(),
                DELTA,
                certificationId,
                estimated
        ));
        assignments.addAssignment(expeditionId, new PersonAssignment(activityId, personId));
        assignments.addPermit(expeditionId, permitId);
        ConsumableId required = estimated.keySet().stream().findFirst().orElse(null);
        return new Prepared(expeditionId, activityId, required);
    }

    private ExpeditionId draft() {
        PersonId personId = registry.registerPerson("Ada", List.of(), Availability.always());
        return drafts.draft(
                List.of(new Objective("Map wetland")),
                PERIOD,
                List.of(DELTA),
                List.of(personId),
                List.of(new Restriction("Daylight only"))
        );
    }

    private OpenSampling openSampling() {
        CertificationId certificationId = new CertificationId(UUID.randomUUID());
        PersonId personId = registry.registerPerson(
                "Ada",
                List.of(new Certification(certificationId, "Sampling")),
                Availability.always()
        );
        ExpeditionId expeditionId = drafts.draft(
                List.of(new Objective("Map wetland")),
                PERIOD,
                List.of(DELTA),
                List.of(personId),
                List.of(new Restriction("Daylight only"))
        );
        ActivityId activityId = new ActivityId(UUID.randomUUID());
        itinerary.addActivity(expeditionId, Activity.sampling(
                activityId,
                "Soil sampling",
                Duration.ofHours(4),
                RiskLevel.MEDIUM,
                new TimePeriod(DAY, DAY.plus(Duration.ofHours(4))),
                Set.of(),
                DELTA,
                certificationId
        ));
        return new OpenSampling(expeditionId, activityId, personId);
    }

    private record Prepared(ExpeditionId expeditionId, ActivityId activityId, ConsumableId requiredConsumable) {
    }

    private record OpenSampling(ExpeditionId expeditionId, ActivityId activityId, PersonId personId) {
    }
}
