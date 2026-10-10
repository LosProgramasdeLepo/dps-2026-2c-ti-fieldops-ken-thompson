package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.adapters.jpa.JpaRepositoryTest;
import edu.itba.fieldops.adapters.jpa.catalog.CertificationJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.JpaPersonRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.PersonJpaRepository;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionState;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
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
import java.util.Optional;
import java.util.UUID;

import static edu.itba.fieldops.adapters.jpa.expedition.ItineraryDescriptions.describe;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JpaRepositoryTest
class ReplanProposalRepositoryTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Autowired
    private TestEntityManager entities;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ProposalJpaRepository proposalRows;
    @Autowired
    private ExpeditionJpaRepository expeditionRows;
    @Autowired
    private ExpeditionRegistrationJpaRepository registrationRows;
    @Autowired
    private CertificationJpaRepository certificationRows;
    @Autowired
    private PersonJpaRepository personRows;

    private JpaReplanProposalRepository proposals;
    private JpaExpeditionRepository plans;
    private Person ada;
    private Expedition approved;
    private Activity soil;

    @BeforeEach
    void registerAnApprovedPlan() {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        proposals = new JpaReplanProposalRepository(proposalRows, expeditionRows, transactions);
        plans = new JpaExpeditionRepository(expeditionRows, registrationRows, transactions);
        JpaPersonRegistry people = new JpaPersonRegistry(personRows, certificationRows, transactions);
        ada = new Person(people.nextPersonId(), "Ada", List.of(), Availability.always());
        people.save(ada);
        approved = Expedition.draft(plans.nextId(), new ExpeditionCharter(
                List.of(new Objective("Map the delta")),
                new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                List.of(DELTA),
                List.of(ada.id()),
                List.of()
        ));
        soil = Activity.transit()
                .named(plans.nextActivityId(), "Soil")
                .estimated(Duration.ofHours(4), RiskLevel.LOW)
                .in(DELTA, new TimePeriod(DAY, DAY.plus(Duration.ofHours(4))))
                .build();
        approved.addActivity(soil);
        approved.submitForReview();
        approved = approved(approved);
        plans.save(approved);
        reload();
    }

    @Test
    void aPendingProposalKeepsTheIncidentAndTheSuggestedPlan() {
        ReplanProposal proposal = proposal("flooded trail");

        proposals.save(proposal);
        reload();

        ReplanProposal stored = proposals.find(proposal.id()).orElseThrow();
        assertAll(
                () -> assertEquals(ReplanProposal.Decision.PENDING, stored.decision()),
                () -> assertEquals(approved.id(), stored.originalId()),
                () -> assertEquals(proposal.incident(), stored.incident()),
                () -> assertEquals(proposal.suggested().id(), stored.suggested().id()),
                () -> assertEquals(describe(proposal.suggested().items()), describe(stored.suggested().items())),
                () -> assertEquals(Optional.empty(), stored.decidedBy())
        );
    }

    @Test
    void aDecisionKeepsWhoTookItAndWhen() {
        ReplanProposal proposal = proposal("flooded trail");
        proposals.save(proposal);
        reload();

        ReplanProposal stored = proposals.find(proposal.id()).orElseThrow();
        stored.accept(ada.id(), DAY.plus(Duration.ofHours(6)));
        proposals.save(stored);
        reload();

        ReplanProposal decided = proposals.find(proposal.id()).orElseThrow();
        assertAll(
                () -> assertEquals(ReplanProposal.Decision.ACCEPTED, decided.decision()),
                () -> assertEquals(Optional.of(ada.id()), decided.decidedBy()),
                () -> assertEquals(Optional.of(DAY.plus(Duration.ofHours(6))), decided.decidedAt())
        );
    }

    @Test
    void theSuggestedPlanIsNotListedUntilItIsSavedAsAPlan() {
        ReplanProposal proposal = proposal("flooded trail");
        proposals.save(proposal);
        reload();

        Optional<Expedition> beforeAccepting = plans.find(proposal.suggested().id());
        plans.save(proposal.suggested());
        reload();

        assertAll(
                () -> assertEquals(Optional.empty(), beforeAccepting),
                () -> assertEquals(List.of(approved.id(), proposal.suggested().id()), ids(plans.all())),
                () -> assertEquals(proposal.suggested().id(), proposals.find(proposal.id()).orElseThrow().suggested().id())
        );
    }

    @Test
    void listsTheProposalsOfAPlanInTheOrderTheyWereMade() {
        ReplanProposal first = proposal("flooded trail");
        ReplanProposal second = proposal("broken axle");
        proposals.save(first);
        proposals.save(second);
        reload();

        Page<ReplanProposal> page = proposals.of(approved.id(), new PageRequest(0, 10));
        Page<ReplanProposal> none = proposals.of(new ExpeditionId(UUID.randomUUID()), new PageRequest(0, 10));

        assertAll(
                () -> assertEquals(List.of(first.id(), second.id()), page.items().stream().map(ReplanProposal::id).toList()),
                () -> assertEquals(2, page.totalItems()),
                () -> assertEquals(0, none.totalItems())
        );
    }

    @Test
    void rejectsADecisionByAPersonWhoIsNotRegistered() {
        ReplanProposal proposal = proposal("flooded trail");
        proposal.reject(new PersonId(UUID.randomUUID()), DAY.plus(Duration.ofHours(6)));

        proposals.save(proposal);

        assertThrows(PersistenceException.class, this::reload);
    }

    @Test
    void anUnknownProposalIsNotFound() {
        assertEquals(Optional.empty(), proposals.find(new ProposalId(UUID.randomUUID())));
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

    private ReplanProposal proposal(String description) {
        Expedition suggestion = approved.reviseAsDraft(plans.nextId());
        suggestion.delay(soil.id(), Duration.ofHours(2));
        Incident incident = Incident.affecting(soil.id(), description, DAY.plus(Duration.ofHours(1)));
        return new ReplanProposal(proposals.nextId(), incident, suggestion);
    }

    private void reload() {
        entities.flush();
        entities.getEntityManager().createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE").executeUpdate();
        entities.getEntityManager().createNativeQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();
        entities.clear();
    }

    private static List<ExpeditionId> ids(List<Expedition> expeditions) {
        return expeditions.stream().map(Expedition::id).toList();
    }
}
