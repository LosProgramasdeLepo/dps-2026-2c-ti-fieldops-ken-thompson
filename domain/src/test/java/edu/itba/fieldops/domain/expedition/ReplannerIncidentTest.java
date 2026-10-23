package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.ResourceCatalog;
import edu.itba.fieldops.domain.support.StoredPeople;
import edu.itba.fieldops.domain.support.StoredPlans;
import edu.itba.fieldops.domain.tracking.Incident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static edu.itba.fieldops.domain.support.Submissions.submit;
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

        Expedition revision = proposedPlan(prepared, incident);

        assertEquals(window(2, 6), revision.activityOf(prepared.activity.id()).window());
    }

    @Test
    void cancelsWhenTheActivityHasAlreadyStarted() {
        Prepared prepared = approvedSampling();
        prepared.execution.startActivity(
                prepared.expedition.startOf(prepared.activity.id(), DAY, List.of(prepared.expedition))
        );
        Incident incident = Incident.affecting(prepared.activity.id(), "equipment failure", DAY);

        Expedition revision = proposedPlan(prepared, incident);

        assertTrue(revision.activities().isEmpty());
    }

    @Test
    void cancelsWhenTheDelayWouldLeaveThePeriod() {
        Prepared prepared = approvedSampling();
        Incident incident = Incident.affecting(prepared.activity.id(), "storm on site", DAY.plus(Duration.ofDays(10)));

        Expedition revision = proposedPlan(prepared, incident);

        assertTrue(revision.activities().isEmpty());
    }

    @Test
    void replacesResourcesWhenTheActivityHasNotStartedAndCannotBeDelayed() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person ada = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), new Availability(List.of(window(4, 8))));
        Person bob = new Person(new PersonId(UUID.randomUUID()), "Bob", List.of(certification), Availability.always());
        Prepared prepared = approvedSampling(ada, certification, bob);
        Incident incident = Incident.affecting(prepared.activity.id(), "forecast change", DAY);

        Expedition revision = proposedPlan(prepared, incident);

        assertAll(
                () -> assertEquals(window(0, 4), revision.activityOf(prepared.activity.id()).window()),
                () -> assertEquals(List.of(new PersonAssignment(prepared.activity.id(), bob.id())), revision.assignments().all())
        );
    }

    @Test
    void anIncidentWithoutAnActivityCannotBeAnswered() {
        Prepared prepared = approvedSampling();
        Incident general = Incident.of("storm on site", DAY);

        assertThrows(InvalidValue.class, () -> proposedPlan(prepared, general));
    }

    @Test
    void aProposalAnswersAnIncidentRecordedOnTheRun() {
        Prepared prepared = approvedSampling();
        Incident incident = Incident.affecting(prepared.activity.id(), "storm on site", DAY.plus(Duration.ofHours(2)));
        prepared.execution.addIncident(incident);
        Expedition revision = prepared.expedition.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));

        ReplanProposal proposal = replanner.propose(new ProposalId(UUID.randomUUID()), contextOf(prepared, revision), incident, prepared.execution);

        assertAll(
                () -> assertEquals(prepared.expedition.id(), proposal.originalId()),
                () -> assertEquals(revision, proposal.suggested()),
                () -> assertEquals(incident, proposal.incident()),
                () -> assertEquals(ReplanProposal.Decision.PENDING, proposal.decision())
        );
    }

    @Test
    void aProposalDoesNotAnswerAnIncidentTheRunDidNotRecord() {
        Prepared prepared = approvedSampling();
        Incident incident = Incident.affecting(prepared.activity.id(), "storm on site", DAY.plus(Duration.ofHours(2)));
        Expedition revision = prepared.expedition.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
        PlanningContext context = contextOf(prepared, revision);
        ProposalId id = new ProposalId(UUID.randomUUID());

        InvalidValue rejected = assertThrows(InvalidValue.class, () -> replanner.propose(id, context, incident, prepared.execution));

        assertEquals("incident is not recorded on the run", rejected.getMessage());
        assertEquals(window(0, 4), revision.activityOf(prepared.activity.id()).window());
    }

    private Expedition proposedPlan(Prepared prepared, Incident incident) {
        prepared.execution.addIncident(incident);
        Expedition revision = prepared.expedition.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
        return replanner.propose(new ProposalId(UUID.randomUUID()), contextOf(prepared, revision), incident, prepared.execution)
                .suggested();
    }

    private static PlanningContext contextOf(Prepared prepared, Expedition revision) {
        return new PlanningContext(revision, prepared.catalog.catalogs(), OccupyingExpeditions.of(revision, List.of(), Map.of()));
    }

    private static Prepared approvedSampling() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person ada = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        return approvedSampling(ada, certification);
    }

    private static Prepared approvedSampling(Person assignee, Certification certification, Person... others) {
        Activity activity = Activity.sampling(certification.id())
                .named(new ActivityId(UUID.randomUUID()), "Soil sampling")
                .estimated(new EstimatedDuration(Duration.ofHours(4)), RiskLevel.MEDIUM)
                .in(DELTA, window(0, 4))
                .build();
        Expedition expedition = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        week(),
                        List.of(DELTA),
                        List.of(assignee.id()),
                        List.of(new Restriction("Daylight only"))
                ),
                StoredPeople.of(assignee)
        );
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(assignee);
        for (Person other : others) {
            catalog.save(other);
        }
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), assignee.id()), catalog.catalogs());
        submit(expedition);
        expedition = StoredPlans.approved(expedition);
        return new Prepared(expedition, activity, assignee, expedition.start(DAY), catalog);
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
