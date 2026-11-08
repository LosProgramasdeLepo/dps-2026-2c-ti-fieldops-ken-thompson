package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.event.ExpeditionApproved;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.ResourceCatalog;
import edu.itba.fieldops.domain.support.StoredExpeditions;
import edu.itba.fieldops.domain.support.StoredPeople;
import edu.itba.fieldops.domain.support.StoredRuns;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static edu.itba.fieldops.domain.support.Submissions.submit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpeditionApproverTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final Instant NOW = DAY.minusSeconds(120);
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final PlanningContexts CONTEXTS =
            new PlanningContexts(new StoredExpeditions(), new StoredRuns(), new ResourceCatalog().catalogs());

    @Test
    void approvesAtTheTimeOfTheClock() {
        Expedition plan = inReview();

        approver(clean()).approve(plan, Optional.empty());

        assertEquals(ExpeditionStatus.APPROVED, plan.status());
        assertEquals(List.of(new ExpeditionApproved(plan.id(), NOW, 1, Optional.empty())), plan.pullEvents());
    }

    @Test
    void aCriticalIssueBlocksTheApprovalAndRegistersNothing() {
        Expedition plan = inReview();
        ExpeditionApprover approver = approver(planning -> new ValidationResult(
                planning.plan().id(),
                planning.plan().version(),
                List.of(new ValidationIssue(IssueSeverity.CRITICAL, "RESOURCE", "missing personnel"))
        ));

        assertThrows(ExpeditionNotApprovable.class, () -> approver.approve(plan, Optional.empty()));

        assertEquals(ExpeditionStatus.IN_REVIEW, plan.status());
        assertEquals(List.of(), plan.pullEvents());
    }

    private static ExpeditionApprover approver(ExpeditionValidator validator) {
        return new ExpeditionApprover(validator, CONTEXTS, () -> NOW);
    }

    private static ExpeditionValidator clean() {
        return planning -> new ValidationResult(planning.plan().id(), planning.plan().version(), List.of());
    }

    private static Expedition inReview() {
        PersonId lead = new PersonId(UUID.randomUUID());
        Expedition plan = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                        List.of(DELTA),
                        List.of(lead),
                        List.of(new Restriction("Daylight only"))
                ),
                StoredPeople.of(new Person(lead, new PersonName("Ada"), List.of(), Availability.always()))
        );
        plan.addActivity(Activity.sampling(new CertificationId(UUID.randomUUID()))
                .named(new ActivityId(UUID.randomUUID()), new ActivityName("Soil sampling"))
                .estimated(new EstimatedDuration(Duration.ofHours(4)), RiskLevel.MEDIUM)
                .in(DELTA, new TimePeriod(DAY, DAY.plus(Duration.ofHours(4))))
                .build());
        submit(plan);
        return plan;
    }
}
