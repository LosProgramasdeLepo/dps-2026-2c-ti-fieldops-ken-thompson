package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.details.ResourceCatalog;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplanProposerTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    private final ReplanProposer proposer = new ReplanProposer(new Replanner(new AssignmentSuggester()));

    @Test
    void delaysAnUnstartedActivityWhenTheIncidentIsAfterItsWindow() {
        Prepared prepared = approvedSampling();
        Incident incident = new Incident("storm on site", DAY.plus(Duration.ofHours(2)), prepared.activity.id());

        ReplanProposal proposal = proposer.propose(
                prepared.expedition,
                prepared.execution,
                incident,
                prepared.catalog.bookable(),
                OccupyingExpeditions.none(),
                new ProposalId(UUID.randomUUID())
        );

        assertAll(
                () -> assertEquals(ExpeditionStatus.APPROVED, prepared.expedition.status()),
                () -> assertEquals(window(0, 4), prepared.expedition.activityOf(prepared.activity.id()).window()),
                () -> assertEquals(ExpeditionStatus.DRAFT, proposal.suggested().status()),
                () -> assertEquals(window(2, 6), proposal.suggested().activityOf(prepared.activity.id()).window()),
                () -> assertEquals(Optional.of(prepared.expedition.id()), proposal.suggested().supersedes()),
                () -> assertEquals(incident, proposal.incident())
        );
    }

    @Test
    void cancelsWhenTheActivityHasAlreadyStarted() {
        Prepared prepared = approvedSampling();
        prepared.execution.startActivity(prepared.activity.id(), DAY, prepared.activity.predecessors());
        Incident incident = new Incident("equipment failure", DAY, prepared.activity.id());

        ReplanProposal proposal = proposer.propose(
                prepared.expedition,
                prepared.execution,
                incident,
                prepared.catalog.bookable(),
                OccupyingExpeditions.none(),
                new ProposalId(UUID.randomUUID())
        );

        assertAll(
                () -> assertTrue(proposal.suggested().itinerary().isEmpty()),
                () -> assertEquals(List.of(prepared.activity.id()), prepared.expedition.itinerary().stream().map(Activity::id).toList()),
                () -> assertEquals(1, prepared.execution.executions().size())
        );
    }

    @Test
    void cancelsWhenTheDelayWouldLeaveThePeriod() {
        Prepared prepared = approvedSampling();
        Incident incident = new Incident("storm on site", DAY.plus(Duration.ofDays(10)), prepared.activity.id());

        ReplanProposal proposal = proposer.propose(
                prepared.expedition,
                prepared.execution,
                incident,
                prepared.catalog.bookable(),
                OccupyingExpeditions.none(),
                new ProposalId(UUID.randomUUID())
        );

        assertTrue(proposal.suggested().itinerary().isEmpty());
        assertEquals(List.of(prepared.activity.id()), prepared.expedition.itinerary().stream().map(Activity::id).toList());
    }

    @Test
    void replacesResourcesWhenTheActivityHasNotStartedAndCannotBeDelayed() {
        Prepared prepared = approvedSampling();
        Incident incident = new Incident("forecast change", DAY, prepared.activity.id());

        ReplanProposal proposal = proposer.propose(
                prepared.expedition,
                prepared.execution,
                incident,
                prepared.catalog.bookable(),
                OccupyingExpeditions.none(),
                new ProposalId(UUID.randomUUID())
        );

        assertAll(
                () -> assertEquals(window(0, 4), proposal.suggested().activityOf(prepared.activity.id()).window()),
                () -> assertEquals(List.of(new PersonAssignment(prepared.activity.id(), prepared.person.id())), proposal.suggested().assignments().all())
        );
    }

    private static Prepared approvedSampling() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
        Activity activity = Activity.sampling(
                new ActivityId(UUID.randomUUID()),
                "Soil sampling",
                Duration.ofHours(4),
                RiskLevel.MEDIUM,
                window(0, 4),
                Set.of(),
                DELTA,
                certification.id()
        );
        Permit permit = Permit.zone(new PermitId(UUID.randomUUID()), DELTA, activity.window());
        Expedition expedition = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                List.of(new Objective("Map wetland")),
                week(),
                List.of(DELTA),
                List.of(person.id()),
                List.of(new Restriction("Daylight only"))
        );
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()));
        expedition.addPermit(permit.id());
        expedition.submitForReview();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);
        catalog.add(permit);
        Approvals.approve(expedition, catalog);
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
