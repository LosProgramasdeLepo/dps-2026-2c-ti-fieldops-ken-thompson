package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.report.OperationalReport;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Observation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingUseCasesTest extends UseCaseFixture {
    @Test
    void rejectsAnExpeditionStartOutsideThePeriod() {
        Sampling sampling = approvedSampling();
        clock.set(at(-1));

        assertThrows(InvalidActivityExecution.class, () -> tracking.start(sampling.expeditionId()));

        assertTrue(runs.find(sampling.expeditionId()).isEmpty());
    }

    @Test
    void rejectsAnActivityStartOutsideItsWindow() {
        Sampling sampling = runningSampling();
        clock.set(at(-1));

        assertThrows(InvalidActivityExecution.class, () -> tracking.startActivity(sampling.expeditionId(), sampling.activityId()));

        assertFalse(run(sampling.expeditionId()).hasStarted(sampling.activityId()));
    }

    @Test
    void closesAnActivityThatRunsPastItsWindow() {
        Sampling sampling = runningSampling();
        tracking.startActivity(sampling.expeditionId(), sampling.activityId());
        clock.set(at(5));

        tracking.finishActivity(sampling.expeditionId(), sampling.activityId(), "samples stored late");

        assertTrue(run(sampling.expeditionId()).activities().getFirst().isFinished());
    }

    @Test
    void anActivityOfASequenceStartsOnlyAfterThePreviousOneFinishes() {
        CertificationId certificationId = certification();
        PersonId ada = certifiedPerson("Ada", certificationId);
        ExpeditionId expeditionId = draftResponsibleFor(ada);
        ActivityId upstream = new ActivityId(UUID.randomUUID());
        ActivityId downstream = new ActivityId(UUID.randomUUID());
        itinerary.addBlock(expeditionId, ActivityBlock.sequential(
                sampling(certificationId, upstream, hours(0, 2)),
                sampling(certificationId, downstream, hours(2, 4))
        ));
        assignments.addAssignment(expeditionId, new PersonAssignment(upstream, ada));
        assignments.addAssignment(expeditionId, new PersonAssignment(downstream, ada));
        assignments.addPermit(expeditionId, registry.registerPermit(DELTA, PERIOD));
        review.submit(expeditionId);
        approval.approve(expeditionId);
        tracking.start(expeditionId);
        clock.set(at(2));

        assertThrows(InvalidActivityExecution.class, () -> tracking.startActivity(expeditionId, downstream));

        clock.set(at(0));
        tracking.startActivity(expeditionId, upstream);
        clock.set(at(2));
        tracking.finishActivity(expeditionId, upstream, "upstream sampled");
        tracking.startActivity(expeditionId, downstream);

        assertTrue(run(expeditionId).hasStarted(downstream));
    }

    @Test
    void aRunCanBeSuspendedResumedAndFinished() {
        Sampling sampling = runningSampling();
        tracking.startActivity(sampling.expeditionId(), sampling.activityId());
        tracking.suspend(sampling.expeditionId());
        tracking.resume(sampling.expeditionId());
        clock.set(at(4));
        tracking.finishActivity(sampling.expeditionId(), sampling.activityId(), "samples stored");

        tracking.finish(sampling.expeditionId());

        assertEquals(ExpeditionExecution.Status.FINISHED, run(sampling.expeditionId()).status());
    }

    @Test
    void recordsAnObservationAtTheCurrentTime() {
        Sampling sampling = runningSampling();
        clock.set(at(1));

        tracking.addObservation(sampling.expeditionId(), "ice on the trail");

        assertEquals(List.of(new Observation("ice on the trail", at(1))), run(sampling.expeditionId()).observations());
    }

    @Test
    void theReportUsesTheActualDurationAndOnlyTheConsumptionOfFinishedActivities() {
        ConsumableId vials = registry.registerConsumable("vials", new Stock(20));
        TwoSamplings plan = approvedTwoSamplingsConsuming(vials);
        tracking.start(plan.expeditionId());
        tracking.startActivity(plan.expeditionId(), plan.first());
        clock.set(at(2));
        tracking.finishActivity(plan.expeditionId(), plan.first(), "samples stored");

        OperationalReport report = reports.of(plan.expeditionId());

        assertEquals(Duration.ofHours(2), report.duration());
        assertEquals(Map.of(vials, new Stock(5)), report.consumption());
    }

    @Test
    void theRunFollowsTheOriginalWhileTheRevisionIsADraft() {
        TwoSamplings plan = startedTwoSamplings();
        replan.cancel(plan.expeditionId(), plan.later());
        clock.set(at(4));
        tracking.finishActivity(plan.expeditionId(), plan.first(), "samples stored");

        tracking.startActivity(plan.expeditionId(), plan.later());

        assertTrue(run(plan.expeditionId()).hasStarted(plan.later()));
    }

    @Test
    void theRunFollowsTheApprovedRevisionAndStillClosesStartedWork() {
        TwoSamplings plan = startedTwoSamplings();
        ExpeditionId revision = replan.cancel(plan.expeditionId(), plan.later());
        review.submit(revision);
        approval.approve(revision);
        assertThrows(InvalidItinerary.class, () -> tracking.startActivity(plan.expeditionId(), plan.later()));
        clock.set(at(4));
        tracking.finishActivity(plan.expeditionId(), plan.first(), "samples stored");

        tracking.finish(plan.expeditionId());

        assertEquals(ExpeditionExecution.Status.FINISHED, run(plan.expeditionId()).status());
    }

    @Test
    void theRunFollowsTheApprovedRevisionEvenWhenAnotherRevisionIsADraft() {
        TwoSamplings plan = startedTwoSamplings();
        replan.cancel(plan.expeditionId(), plan.first());
        ExpeditionId approvedRevision = replan.cancel(plan.expeditionId(), plan.later());
        review.submit(approvedRevision);
        approval.approve(approvedRevision);
        clock.set(at(4));
        tracking.finishActivity(plan.expeditionId(), plan.first(), "samples stored");

        tracking.finish(plan.expeditionId());

        assertEquals(ExpeditionExecution.Status.FINISHED, run(plan.expeditionId()).status());
    }

    @Test
    void aRevisionOfARevisionIsApprovedButNeverStartsASecondRun() {
        TwoSamplings plan = startedTwoSamplings();
        ExpeditionId first = replan.cancel(plan.expeditionId(), plan.first());
        review.submit(first);
        approval.approve(first);
        ExpeditionId second = replan.delay(first, plan.later(), Duration.ofHours(1));
        review.submit(second);

        approval.approve(second);

        assertEquals(ExpeditionStatus.SUPERSEDED, consult.of(first).status());
        assertThrows(InvalidExpeditionTransition.class, () -> tracking.start(second));
        assertEquals(ExpeditionExecution.Status.IN_PROGRESS, run(plan.expeditionId()).status());
    }

    @Test
    void aSupersededPlanOccupiesItsPeopleOnlyWhileItsRunContinues() {
        TwoSamplings plan = startedTwoSamplings();
        ExpeditionId revision = replan.cancel(plan.expeditionId(), plan.first());
        review.submit(revision);
        approval.approve(revision);
        Sampling other = unassignedSampling(Map.of());
        assignments.addAssignment(other.expeditionId(), new PersonAssignment(other.activityId(), plan.firstPerson()));

        assertTrue(hasOverlap(other.expeditionId()));

        clock.set(at(4));
        tracking.finishActivity(plan.expeditionId(), plan.first(), "samples stored");
        tracking.startActivity(plan.expeditionId(), plan.later());
        clock.set(at(6));
        tracking.finishActivity(plan.expeditionId(), plan.later(), "samples stored");
        tracking.finish(plan.expeditionId());

        assertFalse(hasOverlap(other.expeditionId()));
    }

    private TwoSamplings approvedTwoSamplingsConsuming(ConsumableId vials) {
        TwoSamplings plan = twoSamplings();
        assignments.addAssignment(plan.expeditionId(), new ConsumableAssignment(plan.first(), vials, new Stock(5)));
        assignments.addAssignment(plan.expeditionId(), new ConsumableAssignment(plan.later(), vials, new Stock(4)));
        review.submit(plan.expeditionId());
        approval.approve(plan.expeditionId());
        return plan;
    }

    private TwoSamplings startedTwoSamplings() {
        TwoSamplings plan = approvedTwoSamplings();
        tracking.start(plan.expeditionId());
        tracking.startActivity(plan.expeditionId(), plan.first());
        return plan;
    }

    private boolean hasOverlap(ExpeditionId expeditionId) {
        return review.validate(expeditionId).issues().stream().anyMatch(issue -> issue.code().equals("OVERLAP"));
    }

    private ExpeditionExecution run(ExpeditionId expeditionId) {
        return runs.find(expeditionId).orElseThrow();
    }
}
