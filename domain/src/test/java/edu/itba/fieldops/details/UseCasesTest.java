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
import edu.itba.fieldops.domain.expedition.ConsultExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.DraftExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionNotApprovable;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.OccupyingExpeditions;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.PlanItineraryInteractor;
import edu.itba.fieldops.domain.expedition.RecordIncidentInteractor;
import edu.itba.fieldops.domain.expedition.ReplanExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.expedition.Replanner;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.expedition.ReviewExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.ReviewReplanProposalInteractor;
import edu.itba.fieldops.domain.expedition.TrackExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.expedition.usecase.ApproveExpedition;
import edu.itba.fieldops.domain.expedition.usecase.AssignResources;
import edu.itba.fieldops.domain.expedition.usecase.ConsultExpedition;
import edu.itba.fieldops.domain.expedition.usecase.DraftExpedition;
import edu.itba.fieldops.domain.expedition.usecase.PlanItinerary;
import edu.itba.fieldops.domain.expedition.usecase.PlanSnapshot;
import edu.itba.fieldops.domain.expedition.usecase.ProposalSnapshot;
import edu.itba.fieldops.domain.expedition.usecase.RecordIncident;
import edu.itba.fieldops.domain.expedition.usecase.ReplanExpedition;
import edu.itba.fieldops.domain.expedition.usecase.ReviewExpedition;
import edu.itba.fieldops.domain.expedition.usecase.ReviewReplanProposal;
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
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
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
import edu.itba.fieldops.domain.validation.RuleBasedValidator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private final RuleBasedValidator validator = RuleBasedValidator.withDefaultRules();
    private final AdministerCatalog registry = new AdministerCatalogInteractor(catalog, catalog.catalogs());
    private final DraftExpedition drafts = new DraftExpeditionInteractor(plans, catalog);
    private final PlanItinerary itinerary = new PlanItineraryInteractor(plans);
    private final EstimateExpedition estimates = new EstimateExpeditionInteractor(plans);
    private final AssignResources assignments = new AssignResourcesInteractor(plans, runs, catalog.catalogs(), new AssignmentSuggester());
    private final ReviewExpedition review = new ReviewExpeditionInteractor(plans, runs, catalog.catalogs(), validator);
    private final ApproveExpedition approval = new ApproveExpeditionInteractor(plans, runs, catalog.catalogs(), validator);
    private final InMemoryReplanProposalRepository proposals = new InMemoryReplanProposalRepository();
    private final Replanner replanner = new Replanner(new AssignmentSuggester());
    private final TrackExpedition tracking = new TrackExpeditionInteractor(plans, runs, clock);
    private final RecordIncident incidents = new RecordIncidentInteractor(
            plans,
            runs,
            clock,
            catalog.catalogs(),
            replanner,
            proposals
    );
    private final ConsultExpedition consult = new ConsultExpeditionInteractor(plans);
    private final ReviewReplanProposal proposalReview = new ReviewReplanProposalInteractor(plans, proposals, clock);
    private final ReplanExpedition replan = new ReplanExpeditionInteractor(
            plans,
            runs,
            catalog.catalogs(),
            replanner
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
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        PERIOD,
                        List.of(DELTA),
                        List.of(new PersonId(UUID.randomUUID())),
                        List.of(new Restriction("Daylight only"))
                )
        ));
    }

    @Test
    void plansANightActivityAndTheEstimateUsesItsRaisedRisk() {
        CertificationId nightOperation = new CertificationId(UUID.randomUUID());
        PersonId ada = registry.registerPerson("Ada", List.of(new Certification(nightOperation, "Night operation")), Availability.always());
        InstrumentId lamp = registry.registerInstrument(new InstrumentKind("lighting"), Availability.always());
        PermitId permitId = registry.registerNightPermit(DELTA, PERIOD);
        ExpeditionId expeditionId = drafts.draft(
                new ExpeditionCharter(
                        List.of(new Objective("Night survey")),
                        PERIOD,
                        List.of(DELTA),
                        List.of(ada),
                        List.of(new Restriction("Stay on the water"))
                )
        );
        ActivityId activityId = new ActivityId(UUID.randomUUID());
        itinerary.addActivity(expeditionId, Activity.night(nightOperation)
                .named(activityId, "Night survey")
                .estimated(Duration.ofHours(3), RiskLevel.MEDIUM)
                .in(DELTA, new TimePeriod(DAY, DAY.plus(Duration.ofHours(3))))
                .build());
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
    void plansAParallelBlockInsideASequenceAndEstimatesTheMax() {
        CertificationId certificationId = new CertificationId(UUID.randomUUID());
        PersonId ada = registry.registerPerson("Ada", List.of(new Certification(certificationId, "Sampling")), Availability.always());
        ExpeditionId expeditionId = drafts.draft(
                new ExpeditionCharter(
                        List.of(new Objective("Survey the delta")),
                        PERIOD,
                        List.of(DELTA),
                        List.of(ada),
                        List.of(new Restriction("Stay on the water"))
                )
        );
        Activity approach = Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "Approach")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, new TimePeriod(DAY, DAY.plus(Duration.ofHours(2))))
                .build();
        Activity left = Activity.sampling(certificationId)
                .named(new ActivityId(UUID.randomUUID()), "Left bank")
                .estimated(Duration.ofHours(4), RiskLevel.MEDIUM)
                .in(DELTA, new TimePeriod(DAY.plus(Duration.ofHours(2)), DAY.plus(Duration.ofHours(6))))
                .build();
        Activity right = Activity.sampling(certificationId)
                .named(new ActivityId(UUID.randomUUID()), "Right bank")
                .estimated(Duration.ofHours(3), RiskLevel.HIGH)
                .in(DELTA, new TimePeriod(DAY.plus(Duration.ofHours(2)), DAY.plus(Duration.ofHours(5))))
                .build();
        itinerary.addBlock(expeditionId, ActivityBlock.sequential(approach, ActivityBlock.parallel(left, right)));

        Estimate estimate = estimates.of(expeditionId);
        OperationalReport report = reports.of(expeditionId);

        assertEquals(Duration.ofHours(6), estimate.duration());
        assertEquals(RiskLevel.HIGH, estimate.risk());
        assertEquals(3, report.plannedActivities());
        assertEquals(Duration.ofHours(6), report.duration());
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
    void rejectsSubmittingAnEmptyItinerary() {
        ExpeditionId expeditionId = draft();

        assertThrows(InvalidItinerary.class, () -> review.submit(expeditionId));
    }

    @Test
    void onlyAResponsibleCanAcceptAWarning() {
        ExpeditionId expeditionId = crowdedTransitInReview();
        ValidationIssue capacity = review.validate(expeditionId).warnings().getFirst();
        AcceptedWarning byAStranger = new AcceptedWarning(capacity, "backup team", new PersonId(UUID.randomUUID()));

        InvalidValue rejected = assertThrows(InvalidValue.class, () -> review.acceptWarning(expeditionId, byAStranger));

        assertEquals("warning must be accepted by a responsible", rejected.getMessage());
    }

    @Test
    void rejectsAWarningTheValidationDoesNotRaise() {
        ExpeditionId expeditionId = crowdedTransitInReview();
        PersonId responsible = consult.of(expeditionId).charter().responsibles().getFirst();
        ValidationIssue invented = new ValidationIssue(IssueSeverity.WARNING, "CAPACITY", "vehicle near capacity");

        InvalidValue rejected = assertThrows(
                InvalidValue.class,
                () -> review.acceptWarning(expeditionId, new AcceptedWarning(invented, "backup team", responsible))
        );

        assertEquals("not a current warning: CAPACITY", rejected.getMessage());
    }

    @Test
    void approvesOnceTheResponsibleJustifiesTheWarning() {
        ExpeditionId expeditionId = crowdedTransitInReview();
        PersonId responsible = consult.of(expeditionId).charter().responsibles().getFirst();
        ValidationIssue capacity = review.validate(expeditionId).warnings().getFirst();
        assertThrows(ExpeditionNotApprovable.class, () -> approval.approve(expeditionId));

        review.acceptWarning(expeditionId, new AcceptedWarning(capacity, "second trip planned", responsible));
        approval.approve(expeditionId);

        assertEquals(ExpeditionStatus.APPROVED, consult.of(expeditionId).status());
    }

    @Test
    void rejectsAnActivityStartOutsideItsWindow() {
        Prepared prepared = approvedSampling();
        clock.set(DAY.minus(Duration.ofHours(1)));

        assertThrows(InvalidActivityExecution.class, () -> tracking.startActivity(prepared.expeditionId, prepared.activityId));
        assertTrue(runs.find(prepared.expeditionId).orElseThrow().activities().isEmpty());
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
    void replanKeepsTheOriginalApprovedUntilTheRevisionIsApproved() {
        TwoSamplings plan = approvedTwoSamplings();

        ExpeditionId revision = replan.cancel(plan.expeditionId, plan.later);

        assertEquals(ExpeditionStatus.APPROVED, plans.find(plan.expeditionId).orElseThrow().status());
        assertEquals(ExpeditionStatus.DRAFT, plans.find(revision).orElseThrow().status());
        assertTrue(occupying(revision).plans().isEmpty());

        review.submit(revision);
        approval.approve(revision);

        assertEquals(ExpeditionStatus.SUPERSEDED, plans.find(plan.expeditionId).orElseThrow().status());
        assertEquals(ExpeditionStatus.APPROVED, plans.find(revision).orElseThrow().status());
    }

    @Test
    void onlyOneRevisionOfAPlanCanBeApproved() {
        TwoSamplings plan = approvedTwoSamplings();
        ExpeditionId withoutLater = replan.cancel(plan.expeditionId, plan.later);
        ExpeditionId withoutFirst = replan.cancel(plan.expeditionId, plan.first);
        review.submit(withoutLater);
        approval.approve(withoutLater);
        review.submit(withoutFirst);

        assertThrows(InvalidExpeditionTransition.class, () -> approval.approve(withoutFirst));

        assertEquals(ExpeditionStatus.IN_REVIEW, plans.find(withoutFirst).orElseThrow().status());
        assertEquals(ExpeditionStatus.APPROVED, plans.find(withoutLater).orElseThrow().status());
    }

    @Test
    void aSupersededPlanOccupiesOnlyWhileItsRunContinues() {
        TwoSamplings plan = approvedTwoSamplings();
        tracking.start(plan.expeditionId);
        tracking.startActivity(plan.expeditionId, plan.first);
        ExpeditionId revision = replan.cancel(plan.expeditionId, plan.later);
        review.submit(revision);
        approval.approve(revision);
        Prepared other = samplingPlan(Map.of());

        assertTrue(occupyingIds(other.expeditionId).contains(plan.expeditionId));

        clock.set(DAY.plus(Duration.ofHours(4)));
        tracking.finishActivity(plan.expeditionId, plan.first, "samples stored");
        tracking.finish(plan.expeditionId);

        assertFalse(occupyingIds(other.expeditionId).contains(plan.expeditionId));
    }

    @Test
    void aRevisionOfARevisionIsApprovedButNeverStartsASecondRun() {
        TwoSamplings plan = approvedTwoSamplings();
        tracking.start(plan.expeditionId);
        tracking.startActivity(plan.expeditionId, plan.first);
        ExpeditionId first = replan.cancel(plan.expeditionId, plan.first);
        review.submit(first);
        approval.approve(first);
        ExpeditionId second = replan.delay(first, plan.later, Duration.ofHours(1));
        review.submit(second);

        approval.approve(second);

        assertEquals(ExpeditionStatus.SUPERSEDED, plans.find(first).orElseThrow().status());
        assertThrows(InvalidExpeditionTransition.class, () -> tracking.start(second));
        assertTrue(runs.find(second).isEmpty());
        assertEquals(ExpeditionExecution.Status.IN_PROGRESS, runs.find(plan.expeditionId).orElseThrow().status());
    }

    @Test
    void theRunFollowsTheOriginalWhileTheRevisionIsADraft() {
        TwoSamplings plan = approvedTwoSamplings();
        tracking.start(plan.expeditionId);
        tracking.startActivity(plan.expeditionId, plan.first);
        replan.cancel(plan.expeditionId, plan.later);
        clock.set(DAY.plus(Duration.ofHours(4)));
        tracking.finishActivity(plan.expeditionId, plan.first, "samples stored");

        tracking.startActivity(plan.expeditionId, plan.later);

        assertTrue(runs.find(plan.expeditionId).orElseThrow().hasStarted(plan.later));
    }

    @Test
    void finishFollowsTheApprovedRevisionAndStillClosesStartedWork() {
        TwoSamplings plan = approvedTwoSamplings();
        tracking.start(plan.expeditionId);
        tracking.startActivity(plan.expeditionId, plan.first);
        ExpeditionId revision = replan.cancel(plan.expeditionId, plan.later);
        review.submit(revision);
        approval.approve(revision);
        assertThrows(InvalidItinerary.class, () -> tracking.startActivity(plan.expeditionId, plan.later));
        clock.set(DAY.plus(Duration.ofHours(4)));
        tracking.finishActivity(plan.expeditionId, plan.first, "samples stored");

        tracking.finish(plan.expeditionId);

        assertEquals(ExpeditionExecution.Status.FINISHED, runs.find(plan.expeditionId).orElseThrow().status());
    }

    @Test
    void theRunFollowsTheApprovedRevisionEvenWhenAnotherRevisionIsADraft() {
        TwoSamplings plan = approvedTwoSamplings();
        tracking.start(plan.expeditionId);
        tracking.startActivity(plan.expeditionId, plan.first);
        replan.cancel(plan.expeditionId, plan.first);
        ExpeditionId approvedRevision = replan.cancel(plan.expeditionId, plan.later);
        review.submit(approvedRevision);
        approval.approve(approvedRevision);
        clock.set(DAY.plus(Duration.ofHours(4)));
        tracking.finishActivity(plan.expeditionId, plan.first, "samples stored");

        tracking.finish(plan.expeditionId);

        assertEquals(ExpeditionExecution.Status.FINISHED, runs.find(plan.expeditionId).orElseThrow().status());
    }

    @Test
    void closesAnActivityThatRunsPastItsWindow() {
        Prepared prepared = approvedSampling();
        tracking.startActivity(prepared.expeditionId, prepared.activityId);
        clock.set(DAY.plus(Duration.ofHours(5)));

        tracking.finishActivity(prepared.expeditionId, prepared.activityId, "samples stored late");
        tracking.finish(prepared.expeditionId);

        assertEquals(ExpeditionExecution.Status.FINISHED, runs.find(prepared.expeditionId).orElseThrow().status());
    }

    @Test
    void aSequenceIsTrackedInOrder() {
        CertificationId certificationId = new CertificationId(UUID.randomUUID());
        PersonId ada = registry.registerPerson("Ada", List.of(new Certification(certificationId, "Sampling")), Availability.always());
        PermitId permitId = registry.registerPermit(DELTA, PERIOD);
        ExpeditionId expeditionId = drafts.draft(new ExpeditionCharter(
                List.of(new Objective("Walk the transect")),
                PERIOD,
                List.of(DELTA),
                List.of(ada),
                List.of(new Restriction("Daylight only"))
        ));
        ActivityId upstream = new ActivityId(UUID.randomUUID());
        ActivityId downstream = new ActivityId(UUID.randomUUID());
        itinerary.addBlock(expeditionId, ActivityBlock.sequential(
                sampling(certificationId, upstream, new TimePeriod(DAY, DAY.plus(Duration.ofHours(2)))),
                sampling(certificationId, downstream, new TimePeriod(DAY.plus(Duration.ofHours(2)), DAY.plus(Duration.ofHours(4))))
        ));
        assignments.addAssignment(expeditionId, new PersonAssignment(upstream, ada));
        assignments.addAssignment(expeditionId, new PersonAssignment(downstream, ada));
        assignments.addPermit(expeditionId, permitId);
        review.submit(expeditionId);
        approval.approve(expeditionId);
        tracking.start(expeditionId);
        clock.set(DAY.plus(Duration.ofHours(2)));

        assertThrows(InvalidActivityExecution.class, () -> tracking.startActivity(expeditionId, downstream));

        clock.set(DAY);
        tracking.startActivity(expeditionId, upstream);
        clock.set(DAY.plus(Duration.ofHours(2)));
        tracking.finishActivity(expeditionId, upstream, "upstream sampled");
        tracking.startActivity(expeditionId, downstream);
        assertTrue(runs.find(expeditionId).orElseThrow().hasStarted(downstream));
    }

    @Test
    void reportUsesActualDurationAndOnlyTheConsumptionOfFinishedActivities() {
        Prepared prepared = samplingPlan(Map.of());
        ActivityId second = new ActivityId(UUID.randomUUID());
        CertificationId certificationId = new CertificationId(UUID.randomUUID());
        PersonId bob = registry.registerPerson("Bob", List.of(new Certification(certificationId, "Sampling")), Availability.always());
        itinerary.addActivity(prepared.expeditionId, Activity.sampling(certificationId)
                .named(second, "Later sampling")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, new TimePeriod(DAY.plus(Duration.ofHours(4)), DAY.plus(Duration.ofHours(6))))
                .build());
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
        ActivityExecution run = runs.find(prepared.expeditionId).orElseThrow().activities().getFirst();

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
        assertThrows(ExpeditionNotApprovable.class, () -> review.submit(open.expeditionId));
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
        incidents.record(prepared.expeditionId, "storm on site", prepared.activityId);
        tracking.addObservation(prepared.expeditionId, "ice on the trail");
        clock.set(DAY.plus(Duration.ofHours(4)));
        tracking.finishActivity(prepared.expeditionId, prepared.activityId, "samples stored");
        tracking.finish(prepared.expeditionId);

        OperationalReport report = reports.of(prepared.expeditionId);
        ExpeditionExecution execution = runs.find(prepared.expeditionId).orElseThrow();
        assertEquals(ExpeditionExecution.Status.FINISHED, execution.status());
        assertEquals(List.of(Incident.affecting(prepared.activityId, "storm on site", DAY)), execution.incidents());
        assertEquals(List.of(new Observation("ice on the trail", DAY)), execution.observations());
        assertEquals(OperationalStatus.FINISHED, report.status());
        assertEquals(Duration.ofHours(4), report.duration());
    }

    @Test
    void anIncidentOnARunningExpeditionProposesAReplanTheResponsibleCanAccept() {
        Prepared prepared = approvedSampling();
        clock.set(DAY.plus(Duration.ofHours(2)));
        incidents.record(prepared.expeditionId, "storm on site", prepared.activityId);

        List<ProposalSnapshot> found = proposalReview.of(prepared.expeditionId);
        ProposalSnapshot proposal = found.getFirst();
        PersonId ada = plans.find(prepared.expeditionId).orElseThrow().charter().responsibles().getFirst();
        proposalReview.accept(proposal.id(), ada);

        Expedition suggested = plans.find(proposal.suggested().id()).orElseThrow();
        ProposalSnapshot decided = proposalReview.of(prepared.expeditionId).getFirst();
        assertEquals(1, found.size());
        assertEquals(Incident.affecting(prepared.activityId, "storm on site", DAY.plus(Duration.ofHours(2))), proposal.incident());
        assertEquals(ExpeditionStatus.APPROVED, plans.find(prepared.expeditionId).orElseThrow().status());
        assertEquals(ExpeditionStatus.DRAFT, suggested.status());
        assertEquals(
                new TimePeriod(DAY.plus(Duration.ofHours(2)), DAY.plus(Duration.ofHours(6))),
                suggested.activityOf(prepared.activityId).window()
        );
        assertEquals(ReplanProposal.Decision.ACCEPTED, decided.decision());
        assertEquals(ada, decided.decidedBy().orElseThrow());
        assertEquals(List.of(Incident.affecting(prepared.activityId, "storm on site", DAY.plus(Duration.ofHours(2)))), runs.find(prepared.expeditionId).orElseThrow().incidents());

        review.submit(suggested.id());
        approval.approve(suggested.id());

        assertEquals(ExpeditionStatus.SUPERSEDED, plans.find(prepared.expeditionId).orElseThrow().status());
    }

    @Test
    void rejectingAProposalLeavesTheOriginalApprovedAndStillConsultable() {
        Prepared prepared = approvedSampling();
        tracking.startActivity(prepared.expeditionId, prepared.activityId);
        incidents.record(prepared.expeditionId, "equipment failure", prepared.activityId);

        ProposalSnapshot proposal = proposalReview.of(prepared.expeditionId).getFirst();
        PersonId ada = plans.find(prepared.expeditionId).orElseThrow().charter().responsibles().getFirst();
        proposalReview.reject(proposal.id(), ada);

        ProposalSnapshot decided = proposalReview.of(prepared.expeditionId).getFirst();
        assertTrue(proposal.suggested().itinerary().isEmpty());
        assertEquals(ExpeditionStatus.APPROVED, plans.find(prepared.expeditionId).orElseThrow().status());
        assertTrue(plans.find(proposal.suggested().id()).isEmpty());
        assertEquals(ReplanProposal.Decision.REJECTED, decided.decision());
        assertEquals(Incident.affecting(prepared.activityId, "equipment failure", DAY), decided.incident());
        assertEquals(
                List.of("Soil sampling"),
                consult.of(prepared.expeditionId).itinerary().stream()
                        .flatMap(item -> item.activities().stream())
                        .map(Activity::name)
                        .toList()
        );
    }

    @Test
    void anIncidentWithoutAnActivityDoesNotProposeAReplan() {
        Prepared prepared = approvedSampling();
        incidents.record(prepared.expeditionId, "storm on site");

        assertTrue(proposalReview.of(prepared.expeditionId).isEmpty());
        assertEquals(List.of(Incident.of("storm on site", DAY)), runs.find(prepared.expeditionId).orElseThrow().incidents());
    }

    @Test
    void onlyAResponsibleCanDecideAProposal() {
        Prepared prepared = approvedSampling();
        incidents.record(prepared.expeditionId, "storm on site", prepared.activityId);
        ProposalSnapshot proposal = proposalReview.of(prepared.expeditionId).getFirst();

        assertThrows(InvalidValue.class, () -> proposalReview.accept(proposal.id(), new PersonId(UUID.randomUUID())));
        assertEquals(ReplanProposal.Decision.PENDING, proposalReview.of(prepared.expeditionId).getFirst().decision());
        assertEquals(ExpeditionStatus.APPROVED, plans.find(prepared.expeditionId).orElseThrow().status());
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
        assertEquals(ExpeditionStatus.APPROVED, plans.find(approved.expeditionId).orElseThrow().status());
        assertEquals(ExpeditionStatus.DRAFT, plans.find(revision).orElseThrow().status());
        assertEquals(2, plans.find(revision).orElseThrow().version());
    }

    @Test
    void changesAvailabilityCertifiesAndChangesStock() {
        PersonId ada = registry.registerPerson("Ada", List.of(), Availability.always());
        VehicleId boat = registry.registerVehicle(new Passengers(4), Availability.always());
        InstrumentId lamp = registry.registerInstrument(InstrumentKind.LIGHTING, Availability.always());
        ConsumableId vials = registry.registerConsumable("vials", new Stock(20));
        Certification diving = new Certification(new CertificationId(UUID.randomUUID()), "Diving");
        Availability afternoon = new Availability(List.of(new TimePeriod(DAY.plus(Duration.ofHours(6)), DAY.plus(Duration.ofHours(10)))));
        TimePeriod morning = new TimePeriod(DAY, DAY.plus(Duration.ofHours(4)));

        registry.changeAvailability(ada, afternoon);
        registry.changeAvailability(boat, afternoon);
        registry.changeAvailability(lamp, afternoon);
        registry.certify(ada, diving);
        registry.changeStock(vials, new Stock(5));

        assertAll(
                () -> assertFalse(catalog.person(ada).orElseThrow().availableDuring(morning)),
                () -> assertTrue(catalog.person(ada).orElseThrow().holds(diving.id())),
                () -> assertFalse(catalog.vehicle(boat).orElseThrow().availableDuring(morning)),
                () -> assertFalse(catalog.instrument(lamp).orElseThrow().availableDuring(morning)),
                () -> assertEquals(new Stock(5), catalog.consumable(vials).orElseThrow().stock())
        );
    }

    @Test
    void rejectsACertificationThePersonAlreadyHolds() {
        Certification sampling = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        PersonId ada = registry.registerPerson("Ada", List.of(sampling), Availability.always());

        assertThrows(InvalidValue.class, () -> registry.certify(ada, sampling));
    }

    @Test
    void rejectsChangingAnUnknownResource() {
        VehicleId unknown = new VehicleId(UUID.randomUUID());

        assertThrows(InvalidValue.class, () -> registry.changeAvailability(unknown, Availability.always()));
    }

    @Test
    void anUnavailableVehicleIsReplacedThroughTheReplanUseCase() {
        PersonId ada = registry.registerPerson("Ada", List.of(), Availability.always());
        VehicleId boat = registry.registerVehicle(new Passengers(4), Availability.always());
        VehicleId spare = registry.registerVehicle(new Passengers(4), Availability.always());
        Crossing crossing = crossing(ada, boat);
        review.submit(crossing.expeditionId);
        approval.approve(crossing.expeditionId);

        registry.changeAvailability(boat, new Availability(List.of()));
        assertTrue(review.validate(crossing.expeditionId).hasCritical());
        ExpeditionId revision = replan.replaceUnavailable(crossing.expeditionId);

        assertEquals(List.of(new VehicleAssignment(crossing.activityId, spare)), consult.of(revision).assignments());
    }

    @Test
    void consultsAPlanAsASnapshot() {
        Prepared prepared = samplingPlan(Map.of());

        PlanSnapshot snapshot = consult.of(prepared.expeditionId);

        assertAll(
                () -> assertEquals(prepared.expeditionId, snapshot.id()),
                () -> assertEquals(1, snapshot.version()),
                () -> assertTrue(snapshot.supersedes().isEmpty()),
                () -> assertEquals(ExpeditionStatus.DRAFT, snapshot.status()),
                () -> assertEquals(1, snapshot.itinerary().size()),
                () -> assertEquals(1, snapshot.assignments().size()),
                () -> assertEquals(1, snapshot.permits().size())
        );
    }

    private OccupyingExpeditions occupying(ExpeditionId expeditionId) {
        Map<ExpeditionId, ExpeditionExecution> executions = new HashMap<>();
        for (Expedition plan : plans.all()) {
            runs.find(plan.id()).ifPresent(execution -> executions.put(plan.id(), execution));
        }
        return OccupyingExpeditions.of(plans.find(expeditionId).orElseThrow(), plans.all(), executions);
    }

    private List<ExpeditionId> occupyingIds(ExpeditionId expeditionId) {
        return occupying(expeditionId).plans().stream().map(Expedition::id).toList();
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
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        PERIOD,
                        List.of(DELTA),
                        List.of(personId),
                        List.of(new Restriction("Daylight only"))
                )
        );
        ActivityId activityId = new ActivityId(UUID.randomUUID());
        itinerary.addActivity(expeditionId, Activity.sampling(certificationId)
                .named(activityId, "Soil sampling")
                .estimated(Duration.ofHours(4), RiskLevel.MEDIUM)
                .in(DELTA, new TimePeriod(DAY, DAY.plus(Duration.ofHours(4))))
                .consuming(estimated)
                .build());
        assignments.addAssignment(expeditionId, new PersonAssignment(activityId, personId));
        assignments.addPermit(expeditionId, permitId);
        ConsumableId required = estimated.keySet().stream().findFirst().orElse(null);
        return new Prepared(expeditionId, activityId, required);
    }

    private TwoSamplings approvedTwoSamplings() {
        Prepared prepared = samplingPlan(Map.of());
        CertificationId certificationId = new CertificationId(UUID.randomUUID());
        PersonId bob = registry.registerPerson("Bob", List.of(new Certification(certificationId, "Sampling")), Availability.always());
        ActivityId later = new ActivityId(UUID.randomUUID());
        itinerary.addActivity(
                prepared.expeditionId,
                sampling(certificationId, later, new TimePeriod(DAY.plus(Duration.ofHours(4)), DAY.plus(Duration.ofHours(6))))
        );
        assignments.addAssignment(prepared.expeditionId, new PersonAssignment(later, bob));
        review.submit(prepared.expeditionId);
        approval.approve(prepared.expeditionId);
        return new TwoSamplings(prepared.expeditionId, prepared.activityId, later);
    }

    private static Activity sampling(CertificationId certificationId, ActivityId activityId, TimePeriod window) {
        return Activity.sampling(certificationId)
                .named(activityId, "Sampling")
                .estimated(Duration.between(window.start(), window.end()), RiskLevel.LOW)
                .in(DELTA, window)
                .build();
    }

    private Crossing crossing(PersonId responsible, VehicleId vehicle) {
        PermitId permitId = registry.registerPermit(DELTA, PERIOD);
        ExpeditionId expeditionId = drafts.draft(new ExpeditionCharter(
                List.of(new Objective("Cross the delta")),
                PERIOD,
                List.of(DELTA),
                List.of(responsible),
                List.of(new Restriction("Daylight only"))
        ));
        ActivityId activityId = new ActivityId(UUID.randomUUID());
        itinerary.addActivity(expeditionId, Activity.transit()
                .named(activityId, "Crossing")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, new TimePeriod(DAY, DAY.plus(Duration.ofHours(2))))
                .build());
        assignments.addAssignment(expeditionId, new VehicleAssignment(activityId, vehicle));
        assignments.addPermit(expeditionId, permitId);
        return new Crossing(expeditionId, activityId);
    }

    private ExpeditionId crowdedTransitInReview() {
        PersonId ada = registry.registerPerson("Ada", List.of(), Availability.always());
        PersonId bob = registry.registerPerson("Bob", List.of(), Availability.always());
        Crossing crossing = crossing(ada, registry.registerVehicle(new Passengers(1), Availability.always()));
        assignments.addAssignment(crossing.expeditionId, new PersonAssignment(crossing.activityId, ada));
        assignments.addAssignment(crossing.expeditionId, new PersonAssignment(crossing.activityId, bob));
        review.submit(crossing.expeditionId);
        return crossing.expeditionId;
    }

    private ExpeditionId draft() {
        PersonId personId = registry.registerPerson("Ada", List.of(), Availability.always());
        return drafts.draft(
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        PERIOD,
                        List.of(DELTA),
                        List.of(personId),
                        List.of(new Restriction("Daylight only"))
                )
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
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        PERIOD,
                        List.of(DELTA),
                        List.of(personId),
                        List.of(new Restriction("Daylight only"))
                )
        );
        ActivityId activityId = new ActivityId(UUID.randomUUID());
        itinerary.addActivity(expeditionId, Activity.sampling(certificationId)
                .named(activityId, "Soil sampling")
                .estimated(Duration.ofHours(4), RiskLevel.MEDIUM)
                .in(DELTA, new TimePeriod(DAY, DAY.plus(Duration.ofHours(4))))
                .build());
        return new OpenSampling(expeditionId, activityId, personId);
    }

    private record Prepared(ExpeditionId expeditionId, ActivityId activityId, ConsumableId requiredConsumable) {
    }

    private record TwoSamplings(ExpeditionId expeditionId, ActivityId first, ActivityId later) {
    }

    private record Crossing(ExpeditionId expeditionId, ActivityId activityId) {
    }

    private record OpenSampling(ExpeditionId expeditionId, ActivityId activityId, PersonId personId) {
    }
}
