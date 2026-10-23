package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.ExecutionState;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.OccupyingExpeditions;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.PlanningContext;
import edu.itba.fieldops.domain.expedition.PlanningContexts;
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
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.Capacity;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.ResourceCatalog;
import edu.itba.fieldops.domain.support.StoredExpeditions;
import edu.itba.fieldops.domain.support.StoredPeople;
import edu.itba.fieldops.domain.support.StoredRuns;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.ActivityReport;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionValidatorTest {
    private final RuleBasedValidator validator = RuleBasedValidator.withDefaultRules();

    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final InstrumentKind PROBE = new InstrumentKind("probe");
    private static final Certification NIGHT_OPERATION = new Certification(new CertificationId(UUID.randomUUID()), "Night operation");

    @Test
    void validSamplingPlanHasNoIssues() {
        SamplingPlan plan = samplingPlan(0, 4);

        ValidationResult result = validator.validate(contextOf(plan.expedition, plan.catalog));

        assertTrue(result.issues().isEmpty());
    }

    @Test
    void samePersonCanJoinAdjacentExpeditions() {
        SamplingPlan morning = samplingPlan(0, 4);
        Expedition afternoon = samplingOn(morning, 4, 8);
        submit(morning.expedition, morning.catalog);

        ValidationResult result = validator.validate(new PlanningContext(afternoon, morning.catalog.catalogs(), occupying(afternoon, morning.expedition)));

        assertTrue(result.issues().isEmpty());
    }

    @Test
    void overlappingWindowsOnOccupyingExpeditionAreCritical() {
        SamplingPlan first = samplingPlan(0, 4);
        Expedition second = samplingOn(first, 0, 4);
        submit(first.expedition, first.catalog);

        ValidationResult result = validator.validate(new PlanningContext(second, first.catalog.catalogs(), occupying(second, first.expedition)));

        assertIssue(result, IssueSeverity.CRITICAL, "OVERLAP");
    }

    @Test
    void draftPeerDoesNotOccupyThePerson() {
        SamplingPlan first = samplingPlan(0, 4);
        Expedition second = samplingOn(first, 0, 4);

        ValidationResult result = validator.validate(new PlanningContext(second, first.catalog.catalogs(), occupying(second, first.expedition)));

        assertNo(result, "OVERLAP");
    }

    @Test
    void finishedPeerDoesNotOccupyThePerson() {
        SamplingPlan first = samplingPlan(0, 4);
        Expedition second = samplingOn(first, 0, 4);
        ExpeditionExecution finished = finish(first.expedition, first.activity);

        ValidationResult result = validator.validate(new PlanningContext(second, first.catalog.catalogs(), OccupyingExpeditions.of(second, List.of(first.expedition), Map.of(first.expedition.id(), finished))));

        assertNo(result, "OVERLAP");
    }

    @Test
    void adjacentActivitiesInTheSameExpeditionDoNotOverlap() {
        SamplingPlan plan = samplingPlan(0, 4);
        Activity later = sampling(plan.certification.id(), 4, 8);
        plan.expedition.addActivity(later);
        plan.expedition.addAssignment(new PersonAssignment(later.id(), plan.person.id()), plan.catalog.catalogs());

        ValidationResult result = validator.validate(contextOf(plan.expedition, plan.catalog));

        assertNo(result, "OVERLAP");
    }

    @Test
    void overlappingActivitiesInTheSameExpeditionAreCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Activity later = sampling(plan.certification.id(), 2, 6);
        plan.expedition.addActivity(later);
        plan.expedition.addAssignment(new PersonAssignment(later.id(), plan.person.id()), plan.catalog.catalogs());

        ValidationResult result = validator.validate(contextOf(plan.expedition, plan.catalog));

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

        ValidationResult result = validator.validate(contextOf(plan.expedition, plan.catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "AVAILABILITY");
    }

    @Test
    void samplingWithoutPersonIsResourceNotCertification() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Activity activity = sampling(certification.id(), 0, 4);
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(permit);
        expedition.addActivity(activity);
        expedition.addPermit(permit.id(), catalog);

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "CERTIFICATION");
    }

    @Test
    void transitWithoutVehicleIsCritical() {
        Expedition expedition = draft();
        Activity activity = transit(4, 6);
        Permit permit = permitFor(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(permit);
        expedition.addActivity(activity);
        expedition.addPermit(permit.id(), catalog);

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
    }

    @Test
    void measurementWithoutInstrumentIsCritical() {
        Certification operator = new Certification(new CertificationId(UUID.randomUUID()), "Operator");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(operator), Availability.always());
        Activity activity = measurement(operator.id(), 0, 3);
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(person);
        catalog.save(permit);
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()), catalog.catalogs());
        expedition.addPermit(permit.id(), catalog);

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

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
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(person);
        catalog.save(thermometer);
        catalog.save(permit);
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()), catalog.catalogs());
        expedition.addAssignment(new InstrumentAssignment(activity.id(), thermometer.id()), catalog.catalogs());
        expedition.addPermit(permit.id(), catalog);

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
    }

    @Test
    void unknownAssignedPersonIsCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Activity extra = sampling(plan.certification.id(), 4, 8);
        plan.expedition.addActivity(extra);
        Person stranger = uncertified("Stranger");
        plan.expedition.addAssignment(new PersonAssignment(extra.id(), stranger.id()), elsewhere(stranger).catalogs());

        ValidationResult result = validator.validate(contextOf(plan.expedition, plan.catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "CERTIFICATION");
        assertNo(result, "AVAILABILITY");
    }

    @Test
    void missingCertificationIsCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Person unqualified = new Person(new PersonId(UUID.randomUUID()), "Bob", List.of(), Availability.always());
        plan.catalog.save(unqualified);
        Activity extra = sampling(plan.certification.id(), 4, 8);
        plan.expedition.addActivity(extra);
        plan.expedition.addAssignment(new PersonAssignment(extra.id(), unqualified.id()), plan.catalog.catalogs());

        ValidationResult result = validator.validate(contextOf(plan.expedition, plan.catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "CERTIFICATION");
    }

    @Test
    void nightActivityWithoutPersonnelIsResourceNotCertification() {
        ValidationResult result = validateNightSurvey(PermitKind.NIGHT);

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "CERTIFICATION");
    }

    @Test
    void nightCertificationMustBeHeldByEveryAssignee() {
        ValidationResult result = validateNightSurvey(PermitKind.NIGHT, nightOperator("Ada"), uncertified("Bob"));

        assertIssue(result, IssueSeverity.CRITICAL, "CERTIFICATION");
    }

    @Test
    void zonePermitDoesNotCoverANightActivity() {
        ValidationResult result = validateNightSurvey(PermitKind.ZONE, nightOperator("Ada"));

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
        assertNo(result, "CERTIFICATION");
    }

    @Test
    void nightActivityWithCertificationLightingAndNightPermitHasNoIssues() {
        ValidationResult result = validateNightSurvey(PermitKind.NIGHT, nightOperator("Ada"), nightOperator("Bob"));

        assertTrue(result.issues().isEmpty());
    }

    @Test
    void parallelBranchesSharingAPersonAreCriticalEvenWithDisjointWindows() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person ada = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity left = sampling(certification.id(), 0, 4);
        Activity right = sampling(certification.id(), 4, 8);
        Permit leftPermit = permitFor(left);
        Permit rightPermit = permitFor(right);
        Expedition expedition = draft();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(leftPermit);
        catalog.save(rightPermit);
        expedition.addBlock(ActivityBlock.parallel(left, right));
        expedition.addAssignment(new PersonAssignment(left.id(), ada.id()), catalog.catalogs());
        expedition.addAssignment(new PersonAssignment(right.id(), ada.id()), catalog.catalogs());
        expedition.addPermit(leftPermit.id(), catalog);
        expedition.addPermit(rightPermit.id(), catalog);

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "PARALLEL");
        assertNo(result, "OVERLAP");
    }

    @Test
    void sequentialBlockMayReuseAPerson() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person ada = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity left = sampling(certification.id(), 0, 4);
        Activity right = sampling(certification.id(), 4, 8);
        Permit leftPermit = permitFor(left);
        Permit rightPermit = permitFor(right);
        Expedition expedition = draft();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(leftPermit);
        catalog.save(rightPermit);
        expedition.addBlock(ActivityBlock.sequential(left, right));
        expedition.addAssignment(new PersonAssignment(left.id(), ada.id()), catalog.catalogs());
        expedition.addAssignment(new PersonAssignment(right.id(), ada.id()), catalog.catalogs());
        expedition.addPermit(leftPermit.id(), catalog);
        expedition.addPermit(rightPermit.id(), catalog);

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

        assertNo(result, "PARALLEL");
        assertNo(result, "OVERLAP");
    }

    @Test
    void consumablesOnParallelBranchesAreNotAParallelConflict() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person ada = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Person bob = new Person(new PersonId(UUID.randomUUID()), "Bob", List.of(certification), Availability.always());
        Activity first = sampling(certification.id(), 0, 4);
        Activity second = sampling(certification.id(), 0, 4);
        Permit firstPermit = permitFor(first);
        Permit secondPermit = permitFor(second);
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(20));
        Expedition expedition = draft();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(bob);
        catalog.save(vials);
        catalog.save(firstPermit);
        catalog.save(secondPermit);
        expedition.addBlock(ActivityBlock.parallel(first, second));
        expedition.addAssignment(new PersonAssignment(first.id(), ada.id()), catalog.catalogs());
        expedition.addAssignment(new PersonAssignment(second.id(), bob.id()), catalog.catalogs());
        expedition.addAssignment(new ConsumableAssignment(first.id(), vials.id(), new Consumption(3)), catalog.catalogs());
        expedition.addAssignment(new ConsumableAssignment(second.id(), vials.id(), new Consumption(3)), catalog.catalogs());
        expedition.addPermit(firstPermit.id(), catalog);
        expedition.addPermit(secondPermit.id(), catalog);

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

        assertNo(result, "PARALLEL");
    }

    @Test
    void nightActivityInsideABlockStillRequiresANightPermit() {
        Certification samplingCert = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person ada = new Person(
                new PersonId(UUID.randomUUID()),
                "Ada",
                List.of(NIGHT_OPERATION, samplingCert),
                Availability.always()
        );
        Activity nightSurvey = night(0, 4);
        Activity sample = sampling(samplingCert.id(), 4, 8);
        Instrument lamp = new Instrument(new InstrumentId(UUID.randomUUID()), InstrumentKind.LIGHTING, Availability.always());
        Permit zonePermit = permitFor(nightSurvey);
        Permit samplePermit = permitFor(sample);
        Expedition expedition = draft();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(lamp);
        catalog.save(zonePermit);
        catalog.save(samplePermit);
        expedition.addBlock(ActivityBlock.sequential(nightSurvey, sample));
        expedition.addAssignment(new PersonAssignment(nightSurvey.id(), ada.id()), catalog.catalogs());
        expedition.addAssignment(new InstrumentAssignment(nightSurvey.id(), lamp.id()), catalog.catalogs());
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()), catalog.catalogs());
        expedition.addPermit(zonePermit.id(), catalog);
        expedition.addPermit(samplePermit.id(), catalog);

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
        assertNo(result, "PARALLEL");
    }

    @Test
    void stockShortfallIsCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(10));
        plan.catalog.save(vials);
        plan.expedition.addAssignment(new ConsumableAssignment(plan.activity.id(), vials.id(), new Consumption(15)), plan.catalog.catalogs());

        ValidationResult result = validator.validate(contextOf(plan.expedition, plan.catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "STOCK");
    }

    @Test
    void occupyingPeerConsumesStock() {
        SamplingPlan first = samplingPlan(0, 4);
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(10));
        first.catalog.save(vials);
        first.expedition.addAssignment(new ConsumableAssignment(first.activity.id(), vials.id(), new Consumption(6)), first.catalog.catalogs());
        submit(first.expedition, first.catalog);
        Expedition second = samplingOn(first, 4, 8);
        second.addAssignment(new ConsumableAssignment(second.activities().getFirst().id(), vials.id(), new Consumption(5)), first.catalog.catalogs());

        ValidationResult result = validator.validate(new PlanningContext(second, first.catalog.catalogs(), occupying(second, first.expedition)));

        assertIssue(result, IssueSeverity.CRITICAL, "STOCK");
    }

    @Test
    void selfPassedInOthersDoesNotDoubleCountStock() {
        SamplingPlan plan = samplingPlan(0, 4);
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(10));
        plan.catalog.save(vials);
        plan.expedition.addAssignment(new ConsumableAssignment(plan.activity.id(), vials.id(), new Consumption(8)), plan.catalog.catalogs());

        ValidationResult result = validator.validate(new PlanningContext(plan.expedition, plan.catalog.catalogs(), occupying(plan.expedition, plan.expedition)));

        assertNo(result, "STOCK");
    }

    @Test
    void finishedPeerDoesNotConsumeStock() {
        SamplingPlan first = samplingPlan(0, 4);
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(10));
        first.catalog.save(vials);
        first.expedition.addAssignment(new ConsumableAssignment(first.activity.id(), vials.id(), new Consumption(10)), first.catalog.catalogs());
        ExpeditionExecution finished = finish(first.expedition, first.activity);
        Expedition second = samplingOn(first, 4, 8);
        second.addAssignment(new ConsumableAssignment(second.activities().getFirst().id(), vials.id(), new Consumption(10)), first.catalog.catalogs());

        ValidationResult result = validator.validate(new PlanningContext(second, first.catalog.catalogs(), OccupyingExpeditions.of(second, List.of(first.expedition), Map.of(first.expedition.id(), finished))));

        assertNo(result, "STOCK");
    }

    @Test
    void excessCapacityIsAWarning() {
        TransitPlan plan = crowdedTransit();

        ValidationResult result = validator.validate(contextOf(plan.expedition, plan.catalog));

        assertIssue(result, IssueSeverity.WARNING, "CAPACITY");
        assertFalse(result.hasCritical());
    }

    @Test
    void combinedVehicleCapacityCanCarryThePeople() {
        TransitPlan plan = crowdedTransit();
        Activity activity = plan.expedition.activities().getFirst();
        Vehicle extra = new Vehicle(new VehicleId(UUID.randomUUID()), new Capacity(1), Availability.always());
        plan.catalog.save(extra);
        plan.expedition.addAssignment(new VehicleAssignment(activity.id(), extra.id()), plan.catalog.catalogs());

        ValidationResult result = validator.validate(contextOf(plan.expedition, plan.catalog));

        assertNo(result, "CAPACITY");
    }

    @Test
    void unknownPermitIsResourceNotCoverage() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draft();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(person);
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()), catalog.catalogs());
        Permit stranger = permitFor(activity);
        expedition.addPermit(stranger.id(), elsewhere(stranger));

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "PERMIT");
    }

    @Test
    void unknownVehicleDoesNotEmitCapacity() {
        TransitPlan plan = crowdedTransit();
        Activity activity = plan.expedition.activities().getFirst();
        Vehicle stranger = new Vehicle(new VehicleId(UUID.randomUUID()), new Capacity(4), Availability.always());
        plan.expedition.addAssignment(new VehicleAssignment(activity.id(), stranger.id()), elsewhere(stranger).catalogs());

        ValidationResult result = validator.validate(contextOf(plan.expedition, plan.catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "CAPACITY");
    }

    @Test
    void missingPermitIsCritical() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draft();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(person);
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()), catalog.catalogs());

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
    }

    @Test
    void permitDoesNotCoverADifferentZone() {
        WorkZone coast = new WorkZone("Coast");
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity activity = Activity.sampling(certification.id())
                .named(new ActivityId(UUID.randomUUID()), "coast sample")
                .estimated(new EstimatedDuration(Duration.ofHours(4)), RiskLevel.MEDIUM)
                .in(coast, window(0, 4))
                .build();
        Permit deltaPermit = new Permit(new PermitId(UUID.randomUUID()), DELTA, activity.window(), PermitKind.ZONE);
        PersonId lead = new PersonId(UUID.randomUUID());
        Expedition expedition = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland biodiversity")),
                        week(),
                        List.of(DELTA, coast),
                        List.of(lead),
                        List.of(new Restriction("No night work"))
                ),
                StoredPeople.of(new Person(lead, "Ada", List.of(), Availability.always()))
        );
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(person);
        catalog.save(deltaPermit);
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()), catalog.catalogs());
        expedition.addPermit(deltaPermit.id(), catalog);

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
    }

    @Test
    void permitThatDoesNotCoverTheWindowIsCritical() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 4, 8);
        Permit morningOnly = new Permit(new PermitId(UUID.randomUUID()), DELTA, window(0, 4), PermitKind.ZONE);
        Expedition expedition = draft();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(person);
        catalog.save(morningOnly);
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()), catalog.catalogs());
        expedition.addPermit(morningOnly.id(), catalog);

        ValidationResult result = validator.validate(contextOf(expedition, catalog));

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
    }

    private ValidationResult validateNightSurvey(PermitKind permitKind, Person... crew) {
        Activity activity = night(0, 4);
        Instrument lamp = new Instrument(new InstrumentId(UUID.randomUUID()), InstrumentKind.LIGHTING, Availability.always());
        Permit permit = new Permit(new PermitId(UUID.randomUUID()), activity.zone(), activity.window(), permitKind);
        Expedition expedition = draft();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(lamp);
        catalog.save(permit);
        expedition.addActivity(activity);
        expedition.addAssignment(new InstrumentAssignment(activity.id(), lamp.id()), catalog.catalogs());
        expedition.addPermit(permit.id(), catalog);
        for (Person person : crew) {
            catalog.save(person);
            expedition.addAssignment(new PersonAssignment(activity.id(), person.id()), catalog.catalogs());
        }
        return validator.validate(contextOf(expedition, catalog));
    }

    private static Person nightOperator(String name) {
        return new Person(new PersonId(UUID.randomUUID()), name, List.of(NIGHT_OPERATION), Availability.always());
    }

    private static Person uncertified(String name) {
        return new Person(new PersonId(UUID.randomUUID()), name, List.of(), Availability.always());
    }

    private void submit(Expedition plan, ResourceCatalog catalog) {
        plan.submitForReview(validator, new PlanningContexts(new StoredExpeditions(), new StoredRuns(), catalog.catalogs()));
    }

    private static ResourceCatalog elsewhere(Person person) {
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(person);
        return catalog;
    }

    private static ResourceCatalog elsewhere(Vehicle vehicle) {
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(vehicle);
        return catalog;
    }

    private static ResourceCatalog elsewhere(Permit permit) {
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(permit);
        return catalog;
    }

    private static PlanningContext contextOf(Expedition plan, ResourceCatalog catalog) {
        return new PlanningContext(plan, catalog.catalogs(), OccupyingExpeditions.of(plan, List.of(), Map.of()));
    }

    private static ExpeditionExecution finish(Expedition expedition, Activity activity) {
        ActivityExecution done = new ActivityExecution(activity.id(), DAY)
                .finish(DAY.plusSeconds(4 * 3600L), ActivityReport.of(new Outcome("samples stored")));
        return ExpeditionExecution.restore(new ExecutionState(
                expedition.id(),
                ExpeditionExecution.Status.FINISHED,
                List.of(done),
                List.of(),
                List.of()
        ));
    }

    private static OccupyingExpeditions occupying(Expedition plan, Expedition other) {
        return OccupyingExpeditions.of(plan, List.of(other), Map.of());
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
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(person);
        catalog.save(permit);
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()), catalog.catalogs());
        expedition.addPermit(permit.id(), catalog);
        return new SamplingPlan(expedition, catalog, activity, person, certification);
    }

    private static Expedition samplingOn(SamplingPlan source, int fromHour, int toHour) {
        Activity activity = sampling(source.certification.id(), fromHour, toHour);
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        expedition.addActivity(activity);
        source.catalog.save(permit);
        expedition.addAssignment(new PersonAssignment(activity.id(), source.person.id()), source.catalog.catalogs());
        expedition.addPermit(permit.id(), source.catalog);
        return expedition;
    }

    private static TransitPlan crowdedTransit() {
        Activity activity = transit(4, 6);
        Permit permit = permitFor(activity);
        Person ada = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(), Availability.always());
        Person bob = new Person(new PersonId(UUID.randomUUID()), "Bob", List.of(), Availability.always());
        Vehicle vehicle = new Vehicle(new VehicleId(UUID.randomUUID()), new Capacity(1), Availability.always());
        Expedition expedition = draft();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(bob);
        catalog.save(vehicle);
        catalog.save(permit);
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), ada.id()), catalog.catalogs());
        expedition.addAssignment(new PersonAssignment(activity.id(), bob.id()), catalog.catalogs());
        expedition.addAssignment(new VehicleAssignment(activity.id(), vehicle.id()), catalog.catalogs());
        expedition.addPermit(permit.id(), catalog);
        return new TransitPlan(expedition, catalog);
    }

    private static Expedition draft() {
        PersonId lead = new PersonId(UUID.randomUUID());
        return Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland biodiversity")),
                        week(),
                        List.of(DELTA),
                        List.of(lead),
                        List.of(new Restriction("No night work"))
                ),
                StoredPeople.of(new Person(lead, "Ada", List.of(), Availability.always()))
        );
    }

    private static Activity night(int fromHour, int toHour) {
        return Activity.night(NIGHT_OPERATION.id())
                .named(new ActivityId(UUID.randomUUID()), "night survey")
                .estimated(new EstimatedDuration(Duration.ofHours(toHour - fromHour)), RiskLevel.LOW)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static Activity sampling(CertificationId certificationId, int fromHour, int toHour) {
        return Activity.sampling(certificationId)
                .named(new ActivityId(UUID.randomUUID()), "sample")
                .estimated(new EstimatedDuration(Duration.ofHours(toHour - fromHour)), RiskLevel.MEDIUM)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static Activity transit(int fromHour, int toHour) {
        return Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "transit")
                .estimated(new EstimatedDuration(Duration.ofHours(toHour - fromHour)), RiskLevel.LOW)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static Activity measurement(CertificationId certificationId, int fromHour, int toHour) {
        return Activity.measurement(certificationId, PROBE)
                .named(new ActivityId(UUID.randomUUID()), "measure")
                .estimated(new EstimatedDuration(Duration.ofHours(toHour - fromHour)), RiskLevel.HIGH)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static Permit permitFor(Activity activity) {
        return new Permit(new PermitId(UUID.randomUUID()), activity.zone(), activity.window(), PermitKind.ZONE);
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
