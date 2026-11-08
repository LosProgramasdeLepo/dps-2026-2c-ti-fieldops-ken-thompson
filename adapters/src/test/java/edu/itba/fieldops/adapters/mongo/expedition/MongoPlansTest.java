package edu.itba.fieldops.adapters.mongo.expedition;

import edu.itba.fieldops.adapters.InMemoryConsumableRegistry;
import edu.itba.fieldops.adapters.InMemoryExecutionRepository;
import edu.itba.fieldops.adapters.InMemoryExpeditionRepository;
import edu.itba.fieldops.adapters.InMemoryInstrumentRegistry;
import edu.itba.fieldops.adapters.InMemoryPermitRegistry;
import edu.itba.fieldops.adapters.InMemoryPersonRegistry;
import edu.itba.fieldops.adapters.InMemoryVehicleRegistry;
import edu.itba.fieldops.adapters.mongo.MongoRepositoryTest;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Capacity;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.ConsumableName;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.catalog.Stock;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionState;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.ExpeditionValidator;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.Justification;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.PlanningContexts;
import edu.itba.fieldops.domain.expedition.ProposalState;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.itinerary.Delay;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static edu.itba.fieldops.adapters.ItineraryDescriptions.describe;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MongoRepositoryTest
class MongoPlansTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final TimePeriod PERIOD = new TimePeriod(DAY, DAY.plus(Duration.ofDays(5)));

    @Autowired
    private MongoTemplate mongo;
    @Autowired
    private ExpeditionMongoRepository expeditionDocuments;
    @Autowired
    private ProposalMongoRepository proposalDocuments;

    private MongoExpeditionRepository plans;
    private MongoReplanProposalRepository proposals;
    private final InMemoryPersonRegistry people = new InMemoryPersonRegistry();
    private final InMemoryVehicleRegistry vehicles = new InMemoryVehicleRegistry();
    private final InMemoryInstrumentRegistry instruments = new InMemoryInstrumentRegistry();
    private final InMemoryConsumableRegistry consumables = new InMemoryConsumableRegistry();
    private final InMemoryPermitRegistry permits = new InMemoryPermitRegistry();
    private final Catalogs catalogs = new Catalogs(new BookableResources(people, vehicles, instruments), consumables, permits);
    private Person ada;

    @BeforeEach
    void connect() {
        mongo.getDb().drop();
        RegistrationSequence sequence = new RegistrationSequence(mongo);
        plans = new MongoExpeditionRepository(expeditionDocuments, sequence);
        proposals = new MongoReplanProposalRepository(proposalDocuments, expeditionDocuments, sequence);
        ada = new Person(people.nextPersonId(), new PersonName("Ada"), List.of(), Availability.always());
        people.save(ada);
    }

    @Test
    void anApprovedPlanKeepsEverythingItWasSavedWith() {
        Expedition approved = approvedPlanWithEveryKindOfData();

        plans.save(approved);

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
    void aRevisionIsListedAfterThePlanItRevises() {
        Expedition approved = approved(draftWith(transit("Ride", 0, 2)));
        Expedition revision = approved.reviseAsDraft(plans.nextId());

        plans.save(approved);
        plans.save(revision);

        Page<Expedition> first = plans.all(new PageRequest(0, 1));
        assertAll(
                () -> assertEquals(List.of(approved.id(), revision.id()), ids(plans.all())),
                () -> assertEquals(List.of(approved.id()), ids(first.items())),
                () -> assertEquals(2, first.totalItems()),
                () -> assertEquals(Optional.of(approved.id()), plans.require(revision.id()).supersedes())
        );
    }

    @Test
    void aSuggestedPlanIsListedOnlyOnceItIsSavedAsAPlan() {
        Expedition approved = approved(draftWith(transit("Ride", 0, 2)));
        plans.save(approved);
        ReplanProposal proposal = proposalFor(approved, "flooded trail");

        proposals.save(proposal);
        Optional<Expedition> beforeAccepting = plans.find(proposal.suggested().id());
        plans.save(proposal.suggested());
        proposals.save(proposal);

        assertAll(
                () -> assertEquals(Optional.empty(), beforeAccepting),
                () -> assertEquals(List.of(approved.id(), proposal.suggested().id()), ids(plans.all())),
                () -> assertEquals(proposal.suggested().id(), proposals.find(proposal.id()).orElseThrow().suggested().id())
        );
    }

    @Test
    void aDecidedProposalKeepsItsIncidentItsSuggestionAndItsDecision() {
        Expedition approved = approved(draftWith(transit("Ride", 0, 2)));
        plans.save(approved);
        ReplanProposal pending = proposalFor(approved, "flooded trail");
        Instant decidedAt = DAY.plus(Duration.ofHours(6));
        ReplanProposal decided = ReplanProposal.restore(new ProposalState(
                pending.id(),
                pending.originalId(),
                pending.suggested().id(),
                pending.incident(),
                ReplanProposal.Decision.ACCEPTED,
                Optional.of(ada.id()),
                Optional.of(decidedAt)
        ), pending.suggested());

        proposals.save(decided);

        ReplanProposal stored = proposals.find(decided.id()).orElseThrow();
        assertAll(
                () -> assertEquals(approved.id(), stored.originalId()),
                () -> assertEquals(decided.incident(), stored.incident()),
                () -> assertEquals(describe(decided.suggested().items()), describe(stored.suggested().items())),
                () -> assertEquals(ReplanProposal.Decision.ACCEPTED, stored.decision()),
                () -> assertEquals(Optional.of(ada.id()), stored.decidedBy()),
                () -> assertEquals(Optional.of(decidedAt), stored.decidedAt())
        );
    }

    @Test
    void listsTheProposalsOfAPlanInTheOrderTheyWereMade() {
        Expedition approved = approved(draftWith(transit("Ride", 0, 2)));
        plans.save(approved);
        ReplanProposal first = proposalFor(approved, "flooded trail");
        ReplanProposal second = proposalFor(approved, "broken axle");
        proposals.save(first);
        proposals.save(second);

        Page<ReplanProposal> page = proposals.of(approved.id(), new PageRequest(0, 10));

        assertAll(
                () -> assertEquals(List.of(first.id(), second.id()), page.items().stream().map(ReplanProposal::id).toList()),
                () -> assertEquals(2, page.totalItems()),
                () -> assertEquals(0, proposals.of(new ExpeditionId(UUID.randomUUID()), new PageRequest(0, 10)).totalItems())
        );
    }

    private Expedition approvedPlanWithEveryKindOfData() {
        Vehicle truck = new Vehicle(vehicles.nextVehicleId(), new Capacity(4), Availability.always());
        Instrument lamp = new Instrument(instruments.nextInstrumentId(), InstrumentKind.LIGHTING, Availability.always());
        Consumable fuel = new Consumable(consumables.nextConsumableId(), new ConsumableName("Fuel"), new Stock(50));
        Permit zone = new Permit(permits.nextPermitId(), DELTA, PERIOD, PermitKind.ZONE);
        Permit night = new Permit(permits.nextPermitId(), DELTA, PERIOD, PermitKind.NIGHT);
        vehicles.save(truck);
        instruments.save(lamp);
        consumables.save(fuel);
        permits.save(zone);
        permits.save(night);
        Activity ride = transit("Ride", 0, 2);
        Activity soil = Activity.sampling(new CertificationId(UUID.randomUUID()))
                .named(plans.nextActivityId(), new ActivityName("Soil"))
                .estimated(new EstimatedDuration(Duration.ofHours(4)), RiskLevel.MEDIUM)
                .consuming(Map.of(fuel.id(), new Consumption(1)))
                .in(DELTA, window(2, 6))
                .after(Set.of(ride.id()))
                .build();
        Activity camp = Activity.camp()
                .named(plans.nextActivityId(), new ActivityName("Camp"))
                .estimated(new EstimatedDuration(Duration.ofHours(2)), RiskLevel.LOW)
                .in(DELTA, window(6, 8))
                .build();
        Activity probe = Activity.measurement(new CertificationId(UUID.randomUUID()), InstrumentKind.LIGHTING)
                .named(plans.nextActivityId(), new ActivityName("Probe"))
                .estimated(new EstimatedDuration(Duration.ofHours(2)), RiskLevel.LOW)
                .in(DELTA, window(8, 10))
                .build();
        Activity watch = Activity.night(new CertificationId(UUID.randomUUID()))
                .named(plans.nextActivityId(), new ActivityName("Night watch"))
                .estimated(new EstimatedDuration(Duration.ofMinutes(90)), RiskLevel.MEDIUM)
                .in(DELTA, window(8, 10))
                .build();
        Expedition plan = draftWith(ride);
        plan.addActivity(soil);
        plan.addBlock(ActivityBlock.sequential(camp, ActivityBlock.parallel(probe, watch)));
        plan.addAssignment(new PersonAssignment(soil.id(), ada.id()), catalogs);
        plan.addAssignment(new VehicleAssignment(ride.id(), truck.id()), catalogs);
        plan.addAssignment(new InstrumentAssignment(watch.id(), lamp.id()), catalogs);
        plan.addAssignment(new ConsumableAssignment(soil.id(), fuel.id(), new Consumption(3)), catalogs);
        plan.addPermit(zone.id(), permits);
        plan.addPermit(night.id(), permits);
        ValidationIssue capacity = new ValidationIssue(IssueSeverity.WARNING, "CAPACITY", "truck near capacity");
        ExpeditionValidator reportingCapacity = context -> new ValidationResult(context.plan().id(), context.plan().version(), List.of(capacity));
        PlanningContexts contexts = new PlanningContexts(new InMemoryExpeditionRepository(), new InMemoryExecutionRepository(), catalogs);
        plan.submitForReview(reportingCapacity, contexts);
        plan.acceptWarning(reportingCapacity, contexts, new AcceptedWarning(capacity, new Justification("a second truck follows"), ada.id()));
        return approved(plan);
    }

    private Expedition draftWith(Activity activity) {
        Expedition draft = Expedition.draft(plans.nextId(), new ExpeditionCharter(
                List.of(new Objective("Map the delta")),
                PERIOD,
                List.of(DELTA),
                List.of(ada.id()),
                List.of(new Restriction("Leave no trace"))
        ), people);
        draft.addActivity(activity);
        return draft;
    }

    private ReplanProposal proposalFor(Expedition approved, String description) {
        Expedition suggestion = approved.reviseAsDraft(plans.nextId());
        Activity first = suggestion.activities().getFirst();
        suggestion.delay(first.id(), new Delay(Duration.ofHours(2)));
        return ReplanProposal.restore(new ProposalState(
                proposals.nextId(),
                approved.id(),
                suggestion.id(),
                new Incident(
                        new IncidentId(UUID.randomUUID()),
                        new IncidentDescription(description),
                        DAY.plus(Duration.ofHours(1)),
                        Optional.of(first.id())
                ),
                ReplanProposal.Decision.PENDING,
                Optional.empty(),
                Optional.empty()
        ), suggestion);
    }

    private Activity transit(String name, int fromHour, int toHour) {
        return Activity.transit()
                .named(plans.nextActivityId(), new ActivityName(name))
                .estimated(new EstimatedDuration(Duration.ofHours(toHour - fromHour)), RiskLevel.LOW)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static Expedition approved(Expedition draft) {
        ExpeditionState state = draft.state();
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

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plus(Duration.ofHours(fromHour)), DAY.plus(Duration.ofHours(toHour)));
    }

    private static List<ExpeditionId> ids(List<Expedition> expeditions) {
        return expeditions.stream().map(Expedition::id).toList();
    }
}
