package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.support.ResourceCatalog;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.Incident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplannerIncidentTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    private final Replanner replanner = new Replanner(new AssignmentSuggester());

    @Test
    void delaysAnUnstartedActivityWhenTheIncidentIsAfterItsWindow() {
        Prepared prepared = approvedSampling();
        Incident incident = Incident.affecting(prepared.activity.id(), "storm on site", DAY.plus(Duration.ofHours(2)));

        Expedition revision = respondTo(prepared, incident);

        assertEquals(window(2, 6), revision.activityOf(prepared.activity.id()).window());
    }

    @Test
    void cancelsWhenTheActivityHasAlreadyStarted() {
        Prepared prepared = approvedSampling();
        prepared.execution.startActivity(prepared.activity.id(), DAY, prepared.activity.predecessors());
        Incident incident = Incident.affecting(prepared.activity.id(), "equipment failure", DAY);

        Expedition revision = respondTo(prepared, incident);

        assertTrue(revision.activities().isEmpty());
    }

    @Test
    void cancelsWhenTheDelayWouldLeaveThePeriod() {
        Prepared prepared = approvedSampling();
        Incident incident = Incident.affecting(prepared.activity.id(), "storm on site", DAY.plus(Duration.ofDays(10)));

        Expedition revision = respondTo(prepared, incident);

        assertTrue(revision.activities().isEmpty());
    }

    @Test
    void replacesResourcesWhenTheActivityHasNotStartedAndCannotBeDelayed() {
        Prepared prepared = approvedSampling();
        Incident incident = Incident.affecting(prepared.activity.id(), "forecast change", DAY);

        Expedition revision = respondTo(prepared, incident);

        assertAll(
                () -> assertEquals(window(0, 4), revision.activityOf(prepared.activity.id()).window()),
                () -> assertEquals(List.of(new PersonAssignment(prepared.activity.id(), prepared.person.id())), revision.assignments().all())
        );
    }

    @Test
    void anIncidentWithoutAnActivityCannotBeAnswered() {
        Prepared prepared = approvedSampling();
        Incident general = Incident.of("storm on site", DAY);

        assertThrows(InvalidValue.class, () -> respondTo(prepared, general));
    }

    private Expedition respondTo(Prepared prepared, Incident incident) {
        Expedition revision = prepared.expedition.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
        replanner.respondTo(
                new PlanningContext(revision, prepared.catalog.catalogs(), OccupyingExpeditions.of(revision, List.of(), Map.of())),
                incident,
                prepared.execution
        );
        return revision;
    }

    private static Prepared approvedSampling() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity activity = Activity.sampling(certification.id())
                .named(new ActivityId(UUID.randomUUID()), "Soil sampling")
                .estimated(Duration.ofHours(4), RiskLevel.MEDIUM)
                .in(DELTA, window(0, 4))
                .build();
        Expedition expedition = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        week(),
                        List.of(DELTA),
                        List.of(person.id()),
                        List.of(new Restriction("Daylight only"))
                )
        );
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()));
        expedition.submitForReview();
        expedition.markApproved();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(person);
        return new Prepared(expedition, activity, person, ExpeditionExecution.started(expedition.id()), catalog);
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plusSeconds(fromHour * 3600L), DAY.plusSeconds(toHour * 3600L));
    }

    private static TimePeriod week() {
        return new TimePeriod(DAY, DAY.plusSeconds(86_400L * 5));
    }

    private record Prepared(
            Expedition expedition,
            Activity activity,
            Person person,
            ExpeditionExecution execution,
            ResourceCatalog catalog
    ) {
    }
}
