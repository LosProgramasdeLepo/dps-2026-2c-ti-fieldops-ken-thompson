package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.adapters.InMemoryExecutionRepository;
import edu.itba.fieldops.adapters.InMemoryExpeditionRepository;
import edu.itba.fieldops.adapters.InMemoryPersonRegistry;
import edu.itba.fieldops.adapters.jpa.JpaRepositoryTest;
import edu.itba.fieldops.adapters.jpa.catalog.CertificationJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.ConsumableJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.InstrumentJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.JpaConsumableRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.JpaInstrumentRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.JpaPermitRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.JpaPersonRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.JpaVehicleRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.PermitJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.PersonJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.VehicleJpaRepository;
import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionState;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.ExpeditionValidator;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.PlanningContexts;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.Delay;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.Capacity;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.shared.WorkZone;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static edu.itba.fieldops.adapters.jpa.expedition.ItineraryDescriptions.describe;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JpaRepositoryTest
class ExpeditionRepositoryTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final TimePeriod PERIOD = new TimePeriod(DAY, DAY.plus(Duration.ofDays(5)));
    private static final CertificationId SAMPLING = new CertificationId(UUID.randomUUID());
    private static final CertificationId NIGHT_OPERATION = new CertificationId(UUID.randomUUID());

    @Autowired
    private TestEntityManager entities;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ExpeditionJpaRepository expeditionRows;
    @Autowired
    private ExpeditionRegistrationJpaRepository registrationRows;
    @Autowired
    private CertificationJpaRepository certificationRows;
    @Autowired
    private PersonJpaRepository personRows;
    @Autowired
    private VehicleJpaRepository vehicleRows;
    @Autowired
    private InstrumentJpaRepository instrumentRows;
    @Autowired
    private ConsumableJpaRepository consumableRows;
    @Autowired
    private PermitJpaRepository permitRows;

    private JpaExpeditionRepository plans;
    private JpaPersonRegistry people;
    private JpaVehicleRegistry vehicles;
    private JpaInstrumentRegistry instruments;
    private JpaConsumableRegistry consumables;
    private JpaPermitRegistry permits;
    private Catalogs catalogs;
    private Person ada;
    private Vehicle truck;
    private Instrument lamp;
    private Consumable fuel;
    private Permit zonePermit;
    private Permit nightPermit;

    @BeforeEach
    void registerTheCatalog() {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        plans = new JpaExpeditionRepository(expeditionRows, registrationRows, transactions);
        people = new JpaPersonRegistry(personRows, certificationRows, transactions);
        vehicles = new JpaVehicleRegistry(vehicleRows, transactions);
        instruments = new JpaInstrumentRegistry(instrumentRows, transactions);
        consumables = new JpaConsumableRegistry(consumableRows, transactions);
        permits = new JpaPermitRegistry(permitRows, transactions);
        catalogs = new Catalogs(new BookableResources(people, vehicles, instruments), consumables, permits);
        ada = new Person(people.nextPersonId(), "Ada", List.of(), Availability.always());
        truck = new Vehicle(vehicles.nextVehicleId(), new Capacity(4), Availability.always());
        lamp = new Instrument(instruments.nextInstrumentId(), InstrumentKind.LIGHTING, Availability.always());
        fuel = new Consumable(consumables.nextConsumableId(), "Fuel", new Stock(50));
        zonePermit = new Permit(permits.nextPermitId(), DELTA, PERIOD, PermitKind.ZONE);
        nightPermit = new Permit(permits.nextPermitId(), DELTA, PERIOD, PermitKind.NIGHT);
        people.save(ada);
        vehicles.save(truck);
        instruments.save(lamp);
        consumables.save(fuel);
        permits.save(zonePermit);
        permits.save(nightPermit);
        reload();
    }

    @Test
    void anApprovedPlanKeepsEverythingItWasSavedWith() {
        Expedition approved = approvedPlan();

        plans.save(approved);
        reload();

        ExpeditionState stored = plans.require(approved.id()).state();
        ExpeditionState original = approved.state();
        assertAll(
                () -> assertEquals(original.version(), stored.version()),
                () -> assertEquals(original.supersedes(), stored.supersedes()),
                () -> assertEquals(ExpeditionStatus.APPROVED, stored.status()),
                () -> assertEquals(original.charter(), stored.charter()),
                () -> assertEquals(describe(original.items()), describe(stored.items())),
                () -> assertEquals(original.assignments(), stored.assignments()),
                () -> assertEquals(original.permits(), stored.permits()),
                () -> assertEquals(original.acceptedWarnings(), stored.acceptedWarnings())
        );
    }

    @Test
    void aRevisionKeepsItsLineageAndPlansAreListedInTheOrderTheyWereSaved() {
        Expedition approved = approvedPlan();
        Expedition revision = approved.reviseAsDraft(plans.nextId());
        revision.delay(revision.activities().getFirst().id(), new Delay(Duration.ofHours(1)));

        plans.save(approved);
        plans.save(revision);
        reload();

        Page<Expedition> first = plans.all(new PageRequest(0, 1));
        Expedition storedRevision = plans.require(revision.id());
        assertAll(
                () -> assertEquals(List.of(approved.id(), revision.id()), ids(plans.all())),
                () -> assertEquals(List.of(approved.id()), ids(first.items())),
                () -> assertEquals(2, first.totalItems()),
                () -> assertEquals(2, storedRevision.version()),
                () -> assertEquals(Optional.of(approved.id()), storedRevision.supersedes()),
                () -> assertEquals(describe(revision.items()), describe(storedRevision.items()))
        );
    }

    @Test
    void savingADraftAgainReplacesItsItineraryInPlace() {
        Expedition draft = draft();
        Activity soil = sampling("Soil", 0, 4, Set.of());
        Activity ride = transit("Ride", 4, 6, Set.of(soil.id()));
        draft.addActivity(soil);
        draft.addActivity(ride);
        draft.addAssignment(new PersonAssignment(ride.id(), ada.id()), catalogs);
        plans.save(draft);
        reload();

        Expedition stored = plans.require(draft.id());
        stored.removeActivity(ride.id());
        Activity camp = camp("Camp", 6, 8);
        stored.addActivity(camp);
        plans.save(stored);
        reload();

        Expedition replaced = plans.require(draft.id());
        assertAll(
                () -> assertEquals(describe(stored.items()), describe(replaced.items())),
                () -> assertEquals(List.of(), replaced.assignments().all()),
                () -> assertEquals(2L, rows("activities", draft.id())),
                () -> assertEquals(0L, rows("activity_predecessors", draft.id())),
                () -> assertEquals(1, plans.all().size())
        );
    }

    @Test
    void aPlanThatWasNeverRegisteredIsNotListed() {
        Expedition approved = approvedPlan();
        Expedition suggestion = approved.reviseAsDraft(plans.nextId());
        plans.save(approved);
        new StoredExpeditions(expeditionRows).save(suggestion);
        reload();

        assertAll(
                () -> assertEquals(Optional.empty(), plans.find(suggestion.id())),
                () -> assertEquals(List.of(approved.id()), ids(plans.all())),
                () -> assertEquals(1, plans.all(new PageRequest(0, 10)).totalItems())
        );
    }

    @Test
    void rejectsAnAssignmentOfAPersonWhoIsNotRegistered() {
        Expedition draft = draft();
        Activity soil = sampling("Soil", 0, 4, Set.of());
        draft.addActivity(soil);
        InMemoryPersonRegistry elsewhere = new InMemoryPersonRegistry();
        Person stranger = new Person(elsewhere.nextPersonId(), "Stranger", List.of(), Availability.always());
        elsewhere.save(stranger);
        draft.addAssignment(
                new PersonAssignment(soil.id(), stranger.id()),
                new Catalogs(new BookableResources(elsewhere, vehicles, instruments), consumables, permits)
        );

        plans.save(draft);

        assertThrows(PersistenceException.class, this::reload);
    }

    @Test
    void anUnknownPlanIsNotFound() {
        ExpeditionId unknown = plans.nextId();

        assertAll(
                () -> assertEquals(Optional.empty(), plans.find(unknown)),
                () -> assertThrows(UnknownResource.class, () -> plans.require(unknown))
        );
    }

    private Expedition approvedPlan() {
        Expedition expedition = draft();
        Activity soil = sampling("Soil", 0, 4, Set.of());
        Activity ride = transit("Ride", 4, 6, Set.of(soil.id()));
        Activity camp = camp("Camp", 6, 8);
        Activity probe = Activity.measurement(SAMPLING, InstrumentKind.LIGHTING)
                .named(plans.nextActivityId(), "Probe")
                .estimated(new EstimatedDuration(Duration.ofHours(2)), RiskLevel.LOW)
                .in(DELTA, window(8, 10))
                .build();
        Activity watch = Activity.night(NIGHT_OPERATION)
                .named(plans.nextActivityId(), "Night watch")
                .estimated(new EstimatedDuration(Duration.ofHours(2)), RiskLevel.MEDIUM)
                .consuming(Map.of(fuel.id(), new Consumption(2)))
                .in(DELTA, window(8, 10))
                .build();
        expedition.addActivity(soil);
        expedition.addActivity(ride);
        expedition.addBlock(ActivityBlock.sequential(camp, ActivityBlock.parallel(probe, watch)));
        expedition.addAssignment(new PersonAssignment(soil.id(), ada.id()), catalogs);
        expedition.addAssignment(new VehicleAssignment(ride.id(), truck.id()), catalogs);
        expedition.addAssignment(new InstrumentAssignment(watch.id(), lamp.id()), catalogs);
        expedition.addAssignment(new ConsumableAssignment(watch.id(), fuel.id(), new Consumption(3)), catalogs);
        expedition.addPermit(zonePermit.id(), permits);
        expedition.addPermit(nightPermit.id(), permits);
        ValidationIssue capacity = new ValidationIssue(IssueSeverity.WARNING, "CAPACITY", "truck near capacity");
        ExpeditionValidator reportingCapacity = planning -> new ValidationResult(planning.plan().id(), planning.plan().version(), List.of(capacity));
        PlanningContexts contexts = new PlanningContexts(new InMemoryExpeditionRepository(), new InMemoryExecutionRepository(), catalogs);
        expedition.submitForReview(reportingCapacity, contexts);
        expedition.acceptWarning(reportingCapacity, contexts, new AcceptedWarning(capacity, "a second truck follows", ada.id()));
        return approved(expedition);
    }

    private static Expedition approved(Expedition inReview) {
        ExpeditionState state = inReview.state();
        return Expedition.restore(new ExpeditionState(
                state.id(),
                state.version(),
                state.supersedes(),
                ExpeditionStatus.APPROVED,
                state.charter(),
                state.items(),
                state.assignments(),
                state.permits(),
                state.acceptedWarnings()
        ));
    }

    private Expedition draft() {
        return Expedition.draft(plans.nextId(), new ExpeditionCharter(
                List.of(new Objective("Map the delta"), new Objective("Sample the soil")),
                PERIOD,
                List.of(DELTA),
                List.of(ada.id()),
                List.of(new Restriction("Leave no trace"))
        ), people);
    }

    private Activity sampling(String name, int fromHour, int toHour, Set<ActivityId> predecessors) {
        return Activity.sampling(SAMPLING)
                .named(plans.nextActivityId(), name)
                .estimated(new EstimatedDuration(Duration.ofHours(toHour - fromHour)), RiskLevel.MEDIUM)
                .consuming(Map.of(fuel.id(), new Consumption(1)))
                .in(DELTA, window(fromHour, toHour))
                .after(predecessors)
                .build();
    }

    private Activity transit(String name, int fromHour, int toHour, Set<ActivityId> predecessors) {
        return Activity.transit()
                .named(plans.nextActivityId(), name)
                .estimated(new EstimatedDuration(Duration.ofHours(toHour - fromHour)), RiskLevel.LOW)
                .in(DELTA, window(fromHour, toHour))
                .after(predecessors)
                .build();
    }

    private Activity camp(String name, int fromHour, int toHour) {
        return Activity.camp()
                .named(plans.nextActivityId(), name)
                .estimated(new EstimatedDuration(Duration.ofHours(toHour - fromHour)), RiskLevel.LOW)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private void reload() {
        entities.flush();
        entities.getEntityManager().createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE").executeUpdate();
        entities.getEntityManager().createNativeQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();
        entities.clear();
    }

    private long rows(String table, ExpeditionId id) {
        return ((Number) entities.getEntityManager()
                .createNativeQuery("SELECT count(*) FROM " + table + " WHERE expedition_id = ?1")
                .setParameter(1, id.value())
                .getSingleResult()).longValue();
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plus(Duration.ofHours(fromHour)), DAY.plus(Duration.ofHours(toHour)));
    }

    private static List<ExpeditionId> ids(List<Expedition> expeditions) {
        return expeditions.stream().map(Expedition::id).toList();
    }
}
