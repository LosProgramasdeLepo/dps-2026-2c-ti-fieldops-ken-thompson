package edu.itba.fieldops.adapters.jpa.tracking;

import edu.itba.fieldops.adapters.jpa.JpaRepositoryTest;
import edu.itba.fieldops.adapters.jpa.catalog.CertificationJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.JpaPersonRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.PersonJpaRepository;
import edu.itba.fieldops.adapters.jpa.expedition.ExpeditionJpaRepository;
import edu.itba.fieldops.adapters.jpa.expedition.ExpeditionRegistrationJpaRepository;
import edu.itba.fieldops.adapters.jpa.expedition.JpaExpeditionRepository;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Observation;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JpaRepositoryTest
class ExecutionRepositoryTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");

    @Autowired
    private TestEntityManager entities;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private RunJpaRepository runRows;
    @Autowired
    private ExpeditionJpaRepository expeditionRows;
    @Autowired
    private ExpeditionRegistrationJpaRepository registrationRows;
    @Autowired
    private CertificationJpaRepository certificationRows;
    @Autowired
    private PersonJpaRepository personRows;

    private JpaExecutionRepository runs;
    private ExpeditionId expeditionId;
    private final ActivityId soil = new ActivityId(UUID.randomUUID());
    private final ActivityId ride = new ActivityId(UUID.randomUUID());

    @BeforeEach
    void registerAPlan() {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        runs = new JpaExecutionRepository(runRows, transactions);
        JpaPersonRegistry people = new JpaPersonRegistry(personRows, certificationRows, transactions);
        JpaExpeditionRepository plans = new JpaExpeditionRepository(expeditionRows, registrationRows, transactions);
        Person ada = new Person(people.nextPersonId(), "Ada", List.of(), Availability.always());
        people.save(ada);
        Expedition plan = Expedition.draft(plans.nextId(), new ExpeditionCharter(
                List.of(new Objective("Map the delta")),
                new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                List.of(new WorkZone("Delta")),
                List.of(ada.id()),
                List.of()
        ));
        plans.save(plan);
        expeditionId = plan.id();
        reload();
    }

    @Test
    void aSuspendedRunKeepsItsActivitiesIncidentsAndObservations() {
        ExpeditionExecution run = ExpeditionExecution.started(expeditionId);
        run.startActivity(soil, DAY, Set.of());
        run.finishActivity(soil, hours(4), "twelve samples");
        run.startActivity(ride, hours(4), Set.of(soil));
        run.addIncident(Incident.affecting(ride, "flooded trail", hours(5)));
        run.addIncident(Incident.of("radio down", hours(5)));
        run.addObservation(new Observation("heron colony", hours(5)));
        run.suspend();

        runs.save(run);
        reload();

        ExpeditionExecution stored = runs.find(expeditionId).orElseThrow();
        assertAll(
                () -> assertEquals(ExpeditionExecution.Status.SUSPENDED, stored.status()),
                () -> assertEquals(List.of(soil, ride), stored.activities().stream().map(ActivityExecution::activityId).toList()),
                () -> assertEquals(Optional.of(hours(4)), stored.activities().getFirst().finishedAt()),
                () -> assertEquals(Optional.of("twelve samples"), stored.activities().getFirst().result()),
                () -> assertEquals(Optional.empty(), stored.activities().get(1).finishedAt()),
                () -> assertEquals(run.incidents(), stored.incidents()),
                () -> assertEquals(run.observations(), stored.observations())
        );
    }

    @Test
    void savingARunAgainKeepsWhatItAlreadyRecorded() {
        ExpeditionExecution run = ExpeditionExecution.started(expeditionId);
        run.startActivity(soil, DAY, Set.of());
        runs.save(run);
        reload();

        ExpeditionExecution stored = runs.find(expeditionId).orElseThrow();
        stored.finishActivity(soil, hours(3), "done");
        stored.finish(Set.of(soil));
        runs.save(stored);
        reload();

        ExpeditionExecution finished = runs.find(expeditionId).orElseThrow();
        assertAll(
                () -> assertEquals(ExpeditionExecution.Status.FINISHED, finished.status()),
                () -> assertEquals(1, finished.activities().size()),
                () -> assertEquals(Optional.of("done"), finished.activities().getFirst().result())
        );
    }

    @Test
    void rejectsARunOfAPlanThatIsNotStored() {
        runs.save(ExpeditionExecution.started(new ExpeditionId(UUID.randomUUID())));

        assertThrows(PersistenceException.class, this::reload);
    }

    @Test
    void aPlanWithoutARunHasNone() {
        assertEquals(Optional.empty(), runs.find(expeditionId));
    }

    private void reload() {
        entities.flush();
        entities.getEntityManager().createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE").executeUpdate();
        entities.getEntityManager().createNativeQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();
        entities.clear();
    }

    private static Instant hours(int hours) {
        return DAY.plus(Duration.ofHours(hours));
    }
}
