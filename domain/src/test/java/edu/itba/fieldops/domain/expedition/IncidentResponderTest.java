package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.CertificationName;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.event.ReplanProposed;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.Incidents;
import edu.itba.fieldops.domain.support.ResourceCatalog;
import edu.itba.fieldops.domain.support.StoredExpeditions;
import edu.itba.fieldops.domain.support.StoredPeople;
import edu.itba.fieldops.domain.support.StoredPlans;
import edu.itba.fieldops.domain.support.StoredProposals;
import edu.itba.fieldops.domain.support.StoredRuns;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static edu.itba.fieldops.domain.support.Submissions.submit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidentResponderTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final Instant PROPOSED_AT = DAY.plusSeconds(90);
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Test
    void delaysAnUnstartedActivityWhenTheIncidentIsAfterItsWindow() {
        Prepared prepared = approvedSampling();
        Incident incident = Incidents.affecting(prepared.activity.id(), new IncidentDescription("storm on site"), DAY.plus(Duration.ofHours(2)));

        Expedition revision = proposedPlan(prepared, incident);

        assertEquals(window(2, 6), revision.activityOf(prepared.activity.id()).window());
    }

    @Test
    void cancelsWhenTheActivityHasAlreadyStarted() {
        Prepared prepared = approvedSampling();
        prepared.execution.startActivity(
                prepared.expedition.startOf(prepared.activity.id(), DAY, List.of(prepared.expedition))
        );
        Incident incident = Incidents.affecting(prepared.activity.id(), new IncidentDescription("equipment failure"), DAY);

        Expedition revision = proposedPlan(prepared, incident);

        assertTrue(revision.activities().isEmpty());
    }

    @Test
    void cancelsWhenTheDelayWouldLeaveThePeriod() {
        Prepared prepared = approvedSampling();
        Incident incident = Incidents.affecting(prepared.activity.id(), new IncidentDescription("storm on site"), DAY.plus(Duration.ofDays(10)));

        Expedition revision = proposedPlan(prepared, incident);

        assertTrue(revision.activities().isEmpty());
    }

    @Test
    void replacesResourcesWhenTheActivityHasNotStartedAndCannotBeDelayed() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), new CertificationName("Sampling"));
        Person ada = new Person(new PersonId(UUID.randomUUID()), new PersonName("Ada"), List.of(certification), new Availability(List.of(window(4, 8))));
        Person bob = new Person(new PersonId(UUID.randomUUID()), new PersonName("Bob"), List.of(certification), Availability.always());
        Prepared prepared = approvedSampling(ada, certification, bob);
        Incident incident = Incidents.affecting(prepared.activity.id(), new IncidentDescription("forecast change"), DAY);

        Expedition revision = proposedPlan(prepared, incident);

        assertAll(
                () -> assertEquals(window(0, 4), revision.activityOf(prepared.activity.id()).window()),
                () -> assertEquals(List.of(new PersonAssignment(prepared.activity.id(), bob.id())), revision.assignments().all())
        );
    }

    @Test
    void anIncidentWithoutAnActivityGetsNoProposal() {
        Prepared prepared = approvedSampling();
        Incident general = Incidents.of(new IncidentDescription("storm on site"), DAY);
        prepared.execution.addIncident(general);

        assertEquals(Optional.empty(), responderOf(prepared).respondTo(prepared.execution, general.id(), newIds()));
    }

    @Test
    void anIncidentOnAnActivityOutsideThePlanInForceGetsNoProposal() {
        Prepared prepared = approvedSampling();
        Incident elsewhere = Incidents.affecting(new ActivityId(UUID.randomUUID()), new IncidentDescription("storm on site"), DAY);
        prepared.execution.addIncident(elsewhere);

        assertEquals(Optional.empty(), responderOf(prepared).respondTo(prepared.execution, elsewhere.id(), newIds()));
    }

    @Test
    void aProposalRevisesThePlanInForceOfTheRun() {
        Prepared prepared = approvedSampling();
        Incident incident = Incidents.affecting(prepared.activity.id(), new IncidentDescription("storm on site"), DAY.plus(Duration.ofHours(2)));
        prepared.execution.addIncident(incident);
        ReplanIds ids = newIds();

        ReplanProposal proposal = responderOf(prepared).respondTo(prepared.execution, incident.id(), ids).orElseThrow();

        assertAll(
                () -> assertEquals(prepared.expedition.id(), proposal.originalId()),
                () -> assertEquals(ids.proposal(), proposal.id()),
                () -> assertEquals(ids.revision(), proposal.suggested().id()),
                () -> assertEquals(ExpeditionStatus.DRAFT, proposal.suggested().status()),
                () -> assertEquals(incident, proposal.incident()),
                () -> assertEquals(ReplanProposal.Decision.PENDING, proposal.decision()),
                () -> assertEquals(ExpeditionStatus.APPROVED, prepared.expedition.status())
        );
    }

    @Test
    void aProposalDoesNotAnswerAnIncidentTheRunDidNotRecord() {
        Prepared prepared = approvedSampling();
        Incident incident = Incidents.affecting(prepared.activity.id(), new IncidentDescription("storm on site"), DAY.plus(Duration.ofHours(2)));
        IncidentResponder responder = responderOf(prepared);
        ReplanIds ids = newIds();

        InvalidValue rejected = assertThrows(InvalidValue.class, () -> responder.respondTo(prepared.execution, incident.id(), ids));

        assertEquals("incident is not recorded on the run", rejected.getMessage());
    }

    @Test
    void theProposalIsDatedByTheClock() {
        Prepared prepared = approvedSampling();
        Incident incident = Incidents.affecting(prepared.activity.id(), new IncidentDescription("storm on site"), DAY.plus(Duration.ofHours(2)));
        prepared.execution.addIncident(incident);

        ReplanProposal proposal = responderOf(prepared).respondTo(prepared.execution, incident.id(), newIds()).orElseThrow();

        assertEquals(
                List.of(new ReplanProposed(prepared.expedition.id(), PROPOSED_AT, proposal.id(), proposal.suggested().id(), incident.id())),
                proposal.pullEvents()
        );
    }

    @Test
    void aSecondResponseToTheSameIncidentCreatesNoOtherProposal() {
        Prepared prepared = approvedSampling();
        Incident incident = Incidents.affecting(prepared.activity.id(), new IncidentDescription("storm on site"), DAY.plus(Duration.ofHours(2)));
        prepared.execution.addIncident(incident);
        StoredProposals proposals = new StoredProposals();
        IncidentResponder responder = responderOf(prepared, new StoredExpeditions(prepared.expedition), proposals);
        ReplanProposal first = responder.respondTo(prepared.execution, incident.id(), newIds()).orElseThrow();
        proposals.save(first);

        assertAll(
                () -> assertEquals(Optional.empty(), responder.respondTo(prepared.execution, incident.id(), newIds())),
                () -> assertEquals(Optional.of(first), proposals.forIncident(incident.id()))
        );
    }

    @Test
    void aPlanInForceThatIsNoLongerApprovedGetsNoProposal() {
        Prepared prepared = approvedSampling();
        Incident incident = Incidents.affecting(prepared.activity.id(), new IncidentDescription("storm on site"), DAY.plus(Duration.ofHours(2)));
        prepared.execution.addIncident(incident);
        Expedition notApproved = draftOf(prepared.expedition.id(), prepared.person, prepared.activity);
        IncidentResponder responder = responderOf(prepared, new StoredExpeditions(notApproved), new StoredProposals());

        assertEquals(Optional.empty(), responder.respondTo(prepared.execution, incident.id(), newIds()));
    }

    private Expedition proposedPlan(Prepared prepared, Incident incident) {
        prepared.execution.addIncident(incident);
        return responderOf(prepared).respondTo(prepared.execution, incident.id(), newIds()).orElseThrow().suggested();
    }

    private static IncidentResponder responderOf(Prepared prepared) {
        return responderOf(prepared, new StoredExpeditions(prepared.expedition), new StoredProposals());
    }

    private static IncidentResponder responderOf(Prepared prepared, StoredExpeditions plans, StoredProposals proposals) {
        StoredRuns runs = new StoredRuns();
        runs.save(prepared.execution);
        PlanningContexts contexts = new PlanningContexts(plans, runs, prepared.catalog.catalogs());
        return new IncidentResponder(plans, proposals, new Replanner(new AssignmentSuggester(), contexts), () -> PROPOSED_AT);
    }

    private static ReplanIds newIds() {
        return new ReplanIds(new ProposalId(UUID.randomUUID()), new ExpeditionId(UUID.randomUUID()));
    }

    private static Prepared approvedSampling() {
        Certification certification = new Certification(new CertificationId(UUID.randomUUID()), new CertificationName("Sampling"));
        Person ada = new Person(new PersonId(UUID.randomUUID()), new PersonName("Ada"), List.of(certification), Availability.always());
        return approvedSampling(ada, certification);
    }

    private static Prepared approvedSampling(Person assignee, Certification certification, Person... others) {
        Activity activity = Activity.sampling(certification.id())
                .named(new ActivityId(UUID.randomUUID()), new ActivityName("Soil sampling"))
                .estimated(new EstimatedDuration(Duration.ofHours(4)), RiskLevel.MEDIUM)
                .in(DELTA, window(0, 4))
                .build();
        Expedition expedition = draftOf(new ExpeditionId(UUID.randomUUID()), assignee, activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(assignee);
        for (Person other : others) {
            catalog.save(other);
        }
        expedition.addAssignment(new PersonAssignment(activity.id(), assignee.id()), catalog.catalogs());
        submit(expedition);
        expedition = StoredPlans.approved(expedition);
        return new Prepared(expedition, activity, assignee, expedition.start(DAY), catalog);
    }

    private static Expedition draftOf(ExpeditionId id, Person responsible, Activity activity) {
        Expedition draft = Expedition.draft(
                id,
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        week(),
                        List.of(DELTA),
                        List.of(responsible.id()),
                        List.of(new Restriction("Daylight only"))
                ),
                StoredPeople.of(responsible)
        );
        draft.addActivity(activity);
        return draft;
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
