package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.details.ResourceCatalog;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.Approvals;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionEditing;
import edu.itba.fieldops.domain.expedition.ExpeditionNotApprovable;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.OccupyingExpeditions;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionValidatorTest {
    private final ExpeditionValidator validator = ExpeditionValidator.withDefaultRules();

    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final InstrumentKind PROBE = new InstrumentKind("probe");

    @Test
    void validSamplingPlanHasNoIssues() {
        SamplingPlan plan = samplingPlan(0, 4);

        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), OccupyingExpeditions.none());

        assertTrue(result.issues().isEmpty());
    }

    @Test
    void samePersonCanJoinAdjacentExpeditions() {
        SamplingPlan morning = samplingPlan(0, 4);
        Expedition afternoon = samplingOn(morning, 4, 8);
        ExpeditionEditing.submitForReview(morning.expedition);

        ValidationResult result = validator.validate(afternoon, morning.catalog.catalogs(), peers(afternoon, morning.expedition));

        assertTrue(result.issues().isEmpty());
    }

    @Test
    void overlappingWindowsOnOccupyingExpeditionAreCritical() {
        SamplingPlan first = samplingPlan(0, 4);
        Expedition second = samplingOn(first, 0, 4);
        ExpeditionEditing.submitForReview(first.expedition);

        ValidationResult result = validator.validate(second, first.catalog.catalogs(), peers(second, first.expedition));

        assertIssue(result, IssueSeverity.CRITICAL, "OVERLAP");
    }

    @Test
    void draftPeerDoesNotOccupyThePerson() {
        SamplingPlan first = samplingPlan(0, 4);
        Expedition second = samplingOn(first, 0, 4);

        ValidationResult result = validator.validate(second, first.catalog.catalogs(), peers(second, first.expedition));

        assertNo(result, "OVERLAP");
    }

    @Test
    void finishedPeerDoesNotOccupyThePerson() {
        SamplingPlan first = samplingPlan(0, 4);
        Expedition second = samplingOn(first, 0, 4);
        ExpeditionExecution finished = finish(first.expedition, first.activity, first.catalog);

        ValidationResult result = validator.validate(
                second,
                first.catalog.catalogs(),
                OccupyingExpeditions.of(second, List.of(first.expedition), Map.of(first.expedition.id(), finished))
        );

        assertEquals(ExpeditionExecution.Status.FINISHED, finished.status());
        assertNo(result, "OVERLAP");
    }

    @Test
    void adjacentActivitiesInTheSameExpeditionDoNotOverlap() {
        SamplingPlan plan = samplingPlan(0, 4);
        Activity later = sampling(plan.certification.id(), 4, 8);
        ExpeditionEditing.addActivity(plan.expedition, later);
        ExpeditionEditing.addAssignment(plan.expedition, new PersonAssignment(later.id(), plan.person.id()));

        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), OccupyingExpeditions.none());

        assertNo(result, "OVERLAP");
    }

    @Test
    void overlappingActivitiesInTheSameExpeditionAreCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Activity later = sampling(plan.certification.id(), 2, 6);
        ExpeditionEditing.addActivity(plan.expedition, later);
        ExpeditionEditing.addAssignment(plan.expedition, new PersonAssignment(later.id(), plan.person.id()));

        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "OVERLAP");
    }

    @Test
    void catalogUnavailabilityIsCritical() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(
                new PersonId(UUID.randomUUID()),
                "Ada",
                List.of(certification),
                new Availability(List.of(window(0, 4)))
        );
        SamplingPlan plan = samplingPlan(person, certification, 4, 8);

        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "AVAILABILITY");
    }

    @Test
    void samplingWithoutPersonIsResourceNotCertification() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Activity activity = sampling(certification.id(), 0, 4);
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addPermit(expedition, permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(permit);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "CERTIFICATION");
    }

    @Test
    void transitWithoutVehicleIsCritical() {
        Expedition expedition = draft();
        Activity activity = transit(4, 6);
        Permit permit = permitFor(activity);
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addPermit(expedition, permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(permit);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
    }

    @Test
    void measurementWithoutInstrumentIsCritical() {
        Certification operator = new Certification(new CertificationId(UUID.randomUUID()), "Operator");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(operator), Availability.always());
        Activity activity = measurement(operator.id(), 0, 3);
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), person.id()));
        ExpeditionEditing.addPermit(expedition, permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);
        catalog.add(permit);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "CERTIFICATION");
    }

    @Test
    void measurementWithTheWrongInstrumentKindIsCritical() {
        Certification operator = new Certification(new CertificationId(UUID.randomUUID()), "Operator");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(operator), Availability.always());
        Instrument thermometer = new Instrument(new InstrumentId(UUID.randomUUID()), new InstrumentKind("thermometer"), Availability.always());
        Activity activity = measurement(operator.id(), 0, 3);
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), person.id()));
        ExpeditionEditing.addAssignment(expedition, new InstrumentAssignment(activity.id(), thermometer.id()));
        ExpeditionEditing.addPermit(expedition, permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);
        catalog.add(thermometer);
        catalog.add(permit);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
    }

    @Test
    void unknownAssignedPersonIsCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Activity extra = sampling(plan.certification.id(), 4, 8);
        ExpeditionEditing.addActivity(plan.expedition, extra);
        ExpeditionEditing.addAssignment(plan.expedition, new PersonAssignment(extra.id(), new PersonId(UUID.randomUUID())));

        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "CERTIFICATION");
        assertNo(result, "AVAILABILITY");
    }

    @Test
    void missingCertificationIsCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Person unqualified = new Person(new PersonId(UUID.randomUUID()), "Bob", List.of(), Availability.always());
        plan.catalog.add(unqualified);
        Activity extra = sampling(plan.certification.id(), 4, 8);
        ExpeditionEditing.addActivity(plan.expedition, extra);
        ExpeditionEditing.addAssignment(plan.expedition, new PersonAssignment(extra.id(), unqualified.id()));

        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "CERTIFICATION");
    }

    @Test
    void nightActivityWithoutPersonnelIsResourceNotCertification() {
        Certification nightOperation = new Certification(new CertificationId(UUID.randomUUID()), "Night operation");
        Activity activity = night(nightOperation.id(), 0, 4);
        Instrument lamp = new Instrument(new InstrumentId(UUID.randomUUID()), new InstrumentKind("lighting"), Availability.always());
        Permit permit = Permit.night(new PermitId(UUID.randomUUID()), activity.zone(), activity.window());
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new InstrumentAssignment(activity.id(), lamp.id()));
        ExpeditionEditing.addPermit(expedition, permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(lamp);
        catalog.add(permit);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "CERTIFICATION");
    }

    @Test
    void nightCertificationMustBeHeldByEveryAssignee() {
        Certification nightOperation = new Certification(new CertificationId(UUID.randomUUID()), "Night operation");
        Person ada = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(nightOperation), Availability.always());
        Person bob = new Person(new PersonId(UUID.randomUUID()), "Bob", List.of(), Availability.always());
        Activity activity = night(nightOperation.id(), 0, 4);
        Instrument lamp = new Instrument(new InstrumentId(UUID.randomUUID()), new InstrumentKind("lighting"), Availability.always());
        Permit permit = Permit.night(new PermitId(UUID.randomUUID()), activity.zone(), activity.window());
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), ada.id()));
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), bob.id()));
        ExpeditionEditing.addAssignment(expedition, new InstrumentAssignment(activity.id(), lamp.id()));
        ExpeditionEditing.addPermit(expedition, permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);
        catalog.add(bob);
        catalog.add(lamp);
        catalog.add(permit);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "CERTIFICATION");
    }

    @Test
    void zonePermitDoesNotCoverANightActivity() {
        Certification nightOperation = new Certification(new CertificationId(UUID.randomUUID()), "Night operation");
        Person ada = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(nightOperation), Availability.always());
        Activity activity = night(nightOperation.id(), 0, 4);
        Instrument lamp = new Instrument(new InstrumentId(UUID.randomUUID()), new InstrumentKind("lighting"), Availability.always());
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), ada.id()));
        ExpeditionEditing.addAssignment(expedition, new InstrumentAssignment(activity.id(), lamp.id()));
        ExpeditionEditing.addPermit(expedition, permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);
        catalog.add(lamp);
        catalog.add(permit);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
        assertNo(result, "CERTIFICATION");
    }

    @Test
    void nightActivityWithCertificationLightingAndNightPermitHasNoIssues() {
        Certification nightOperation = new Certification(new CertificationId(UUID.randomUUID()), "Night operation");
        Person ada = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(nightOperation), Availability.always());
        Person bob = new Person(new PersonId(UUID.randomUUID()), "Bob", List.of(nightOperation), Availability.always());
        Activity activity = night(nightOperation.id(), 0, 4);
        Instrument lamp = new Instrument(new InstrumentId(UUID.randomUUID()), new InstrumentKind("lighting"), Availability.always());
        Permit permit = Permit.night(new PermitId(UUID.randomUUID()), activity.zone(), activity.window());
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), ada.id()));
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), bob.id()));
        ExpeditionEditing.addAssignment(expedition, new InstrumentAssignment(activity.id(), lamp.id()));
        ExpeditionEditing.addPermit(expedition, permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);
        catalog.add(bob);
        catalog.add(lamp);
        catalog.add(permit);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertTrue(result.issues().isEmpty());
    }

    @Test
    void stockShortfallIsCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(10));
        plan.catalog.add(vials);
        ExpeditionEditing.addAssignment(plan.expedition, new ConsumableAssignment(plan.activity.id(), vials.id(), new Stock(15)));

        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "STOCK");
    }

    @Test
    void occupyingPeerConsumesStock() {
        SamplingPlan first = samplingPlan(0, 4);
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(10));
        first.catalog.add(vials);
        ExpeditionEditing.addAssignment(first.expedition, new ConsumableAssignment(first.activity.id(), vials.id(), new Stock(6)));
        ExpeditionEditing.submitForReview(first.expedition);
        Expedition second = samplingOn(first, 4, 8);
        ExpeditionEditing.addAssignment(second, new ConsumableAssignment(second.itinerary().getFirst().id(), vials.id(), new Stock(5)));

        ValidationResult result = validator.validate(second, first.catalog.catalogs(), peers(second, first.expedition));

        assertIssue(result, IssueSeverity.CRITICAL, "STOCK");
    }

    @Test
    void selfPassedInOthersDoesNotDoubleCountStock() {
        SamplingPlan plan = samplingPlan(0, 4);
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(10));
        plan.catalog.add(vials);
        ExpeditionEditing.addAssignment(plan.expedition, new ConsumableAssignment(plan.activity.id(), vials.id(), new Stock(8)));

        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), peers(plan.expedition, plan.expedition));

        assertNo(result, "STOCK");
    }

    @Test
    void finishedPeerDoesNotConsumeStock() {
        SamplingPlan first = samplingPlan(0, 4);
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(10));
        first.catalog.add(vials);
        ExpeditionEditing.addAssignment(first.expedition, new ConsumableAssignment(first.activity.id(), vials.id(), new Stock(10)));
        ExpeditionExecution finished = finish(first.expedition, first.activity, first.catalog);
        Expedition second = samplingOn(first, 4, 8);
        ExpeditionEditing.addAssignment(second, new ConsumableAssignment(second.itinerary().getFirst().id(), vials.id(), new Stock(10)));

        ValidationResult result = validator.validate(
                second,
                first.catalog.catalogs(),
                OccupyingExpeditions.of(second, List.of(first.expedition), Map.of(first.expedition.id(), finished))
        );

        assertNo(result, "STOCK");
    }

    @Test
    void excessCapacityIsAWarning() {
        TransitPlan plan = crowdedTransit();

        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.WARNING, "CAPACITY");
        assertFalse(result.hasCritical());
    }

    @Test
    void combinedVehicleCapacityCanCarryThePeople() {
        TransitPlan plan = crowdedTransit();
        Activity activity = plan.expedition.itinerary().getFirst();
        Vehicle extra = new Vehicle(new VehicleId(UUID.randomUUID()), new Passengers(1), Availability.always());
        plan.catalog.add(extra);
        ExpeditionEditing.addAssignment(plan.expedition, new VehicleAssignment(activity.id(), extra.id()));

        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), OccupyingExpeditions.none());

        assertNo(result, "CAPACITY");
    }

    @Test
    void unknownPermitIsResourceNotCoverage() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), person.id()));
        ExpeditionEditing.addPermit(expedition, new PermitId(UUID.randomUUID()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "PERMIT");
    }

    @Test
    void unknownVehicleDoesNotEmitCapacity() {
        TransitPlan plan = crowdedTransit();
        Activity activity = plan.expedition.itinerary().getFirst();
        ExpeditionEditing.addAssignment(plan.expedition, new VehicleAssignment(activity.id(), new VehicleId(UUID.randomUUID())));

        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "CAPACITY");
    }

    @Test
    void missingPermitIsCritical() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), person.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
    }

    @Test
    void permitDoesNotCoverADifferentZone() {
        WorkZone coast = new WorkZone("Coast");
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity activity = Activity.sampling(
                new ActivityId(UUID.randomUUID()),
                "coast sample",
                Duration.ofHours(4),
                RiskLevel.MEDIUM,
                window(0, 4),
                Set.of(),
                coast,
                certification.id()
        );
        Permit deltaPermit = Permit.zone(new PermitId(UUID.randomUUID()), DELTA, activity.window());
        Expedition expedition = ExpeditionEditing.draft(
                new ExpeditionId(UUID.randomUUID()),
                List.of(new Objective("Map wetland biodiversity")),
                week(),
                List.of(DELTA, coast),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("No night work"))
        );
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), person.id()));
        ExpeditionEditing.addPermit(expedition, deltaPermit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);
        catalog.add(deltaPermit);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
    }

    @Test
    void permitThatDoesNotCoverTheWindowIsCritical() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 4, 8);
        Permit morningOnly = Permit.zone(new PermitId(UUID.randomUUID()), DELTA, window(0, 4));
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), person.id()));
        ExpeditionEditing.addPermit(expedition, morningOnly.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);
        catalog.add(morningOnly);

        ValidationResult result = validator.validate(expedition, catalog.catalogs(), OccupyingExpeditions.none());

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
    }

    @Test
    void approveUsesValidatorResult() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), person.id()));
        ExpeditionEditing.submitForReview(expedition);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);

        assertThrows(
                ExpeditionNotApprovable.class,
                () -> Approvals.approve(expedition, catalog)
        );
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.status());
    }

    @Test
    void capacityWarningCanBeJustifiedAndApproved() {
        TransitPlan plan = crowdedTransit();
        ExpeditionEditing.submitForReview(plan.expedition);
        ValidationResult result = validator.validate(plan.expedition, plan.catalog.catalogs(), OccupyingExpeditions.none());
        result.warnings().forEach(warning -> ExpeditionEditing.acceptWarning(plan.expedition, 
                new AcceptedWarning(warning, "extra trailer available", plan.expedition.responsibles().getFirst())
        ));

        Approvals.approve(plan.expedition, plan.catalog);

        assertEquals(ExpeditionStatus.APPROVED, plan.expedition.status());
    }

    private ExpeditionExecution finish(Expedition expedition, Activity activity, ResourceCatalog catalog) {
        ExpeditionEditing.submitForReview(expedition);
        Approvals.approve(expedition, catalog);
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        execution.startActivity(activity.id(), DAY, expedition.activityOf(activity.id()).predecessors());
        execution.finishActivity(activity.id(), DAY.plusSeconds(4 * 3600L), "samples stored");
        execution.finish(expedition.itinerary());
        return execution;
    }

    private static OccupyingExpeditions peers(Expedition plan, Expedition other) {
        return OccupyingExpeditions.of(plan, List.of(other));
    }

    private static void assertIssue(ValidationResult result, IssueSeverity severity, String code) {
        boolean found = result.issues().stream()
                .anyMatch(issue -> issue.severity() == severity && issue.code().equals(code));
        assertTrue(found, () -> "expected " + severity + " " + code + " in " + result.issues());
    }

    private static void assertNo(ValidationResult result, String code) {
        boolean found = result.issues().stream().anyMatch(issue -> issue.code().equals(code));
        assertFalse(found, () -> "did not expect " + code + " in " + result.issues());
    }

    private static SamplingPlan samplingPlan(int fromHour, int toHour) {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        return samplingPlan(person, certification, fromHour, toHour);
    }

    private static SamplingPlan samplingPlan(Person person, Certification certification, int fromHour, int toHour) {
        Activity activity = sampling(certification.id(), fromHour, toHour);
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), person.id()));
        ExpeditionEditing.addPermit(expedition, permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);
        catalog.add(permit);
        return new SamplingPlan(expedition, catalog, activity, person, certification);
    }

    private static Expedition samplingOn(SamplingPlan source, int fromHour, int toHour) {
        Activity activity = sampling(source.certification.id(), fromHour, toHour);
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), source.person.id()));
        ExpeditionEditing.addPermit(expedition, permit.id());
        source.catalog.add(permit);
        return expedition;
    }

    private static TransitPlan crowdedTransit() {
        Activity activity = transit(4, 6);
        Permit permit = permitFor(activity);
        Person ada = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(), Availability.always());
        Person bob = new Person(new PersonId(UUID.randomUUID()), "Bob", List.of(), Availability.always());
        Vehicle vehicle = new Vehicle(new VehicleId(UUID.randomUUID()), new Passengers(1), Availability.always());
        Expedition expedition = draft();
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), ada.id()));
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), bob.id()));
        ExpeditionEditing.addAssignment(expedition, new VehicleAssignment(activity.id(), vehicle.id()));
        ExpeditionEditing.addPermit(expedition, permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);
        catalog.add(bob);
        catalog.add(vehicle);
        catalog.add(permit);
        return new TransitPlan(expedition, catalog);
    }

    private static Expedition draft() {
        return ExpeditionEditing.draft(
                new ExpeditionId(UUID.randomUUID()),
                List.of(new Objective("Map wetland biodiversity")),
                week(),
                List.of(DELTA),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("No night work"))
        );
    }

    private static Activity night(CertificationId certificationId, int fromHour, int toHour) {
        return Activity.night(
                new ActivityId(UUID.randomUUID()),
                "night survey",
                Duration.ofHours(toHour - fromHour),
                RiskLevel.LOW,
                window(fromHour, toHour),
                Set.of(),
                DELTA,
                certificationId,
                new InstrumentKind("lighting")
        );
    }

    private static Activity sampling(CertificationId certificationId, int fromHour, int toHour) {
        return Activity.sampling(
                new ActivityId(UUID.randomUUID()),
                "sample",
                Duration.ofHours(toHour - fromHour),
                RiskLevel.MEDIUM,
                window(fromHour, toHour),
                Set.of(),
                DELTA,
                certificationId
        );
    }

    private static Activity transit(int fromHour, int toHour) {
        return Activity.transit(
                new ActivityId(UUID.randomUUID()),
                "transit",
                Duration.ofHours(toHour - fromHour),
                RiskLevel.LOW,
                window(fromHour, toHour),
                Set.of(),
                DELTA
        );
    }

    private static Activity measurement(CertificationId certificationId, int fromHour, int toHour) {
        return Activity.measurement(
                new ActivityId(UUID.randomUUID()),
                "measure",
                Duration.ofHours(toHour - fromHour),
                RiskLevel.HIGH,
                window(fromHour, toHour),
                Set.of(),
                DELTA,
                certificationId,
                PROBE
        );
    }

    private static Permit permitFor(Activity activity) {
        return Permit.zone(new PermitId(UUID.randomUUID()), activity.zone(), activity.window());
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plusSeconds(fromHour * 3600L), DAY.plusSeconds(toHour * 3600L));
    }

    private static TimePeriod week() {
        return new TimePeriod(DAY, DAY.plusSeconds(86_400L * 5));
    }

    private record SamplingPlan(
            Expedition expedition,
            ResourceCatalog catalog,
            Activity activity,
            Person person,
            Certification certification
    ) {
    }

    private record TransitPlan(Expedition expedition, ResourceCatalog catalog) {
    }
}
