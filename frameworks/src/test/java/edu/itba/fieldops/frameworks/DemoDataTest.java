package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.FixedClock;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.usecase.expedition.PlanSnapshot;
import edu.itba.fieldops.usecase.expedition.ProposalSnapshot;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDataTest {
    private static final PageRequest ALL = new PageRequest(0, 100);

    private final InMemoryFieldOps memory = new InMemoryFieldOps(new FixedClock(Instant.parse("2026-11-01T10:30:00Z")));
    private final FieldOps useCases = memory.useCases();
    private final DemoData demo = new DemoData(useCases, new FixedClock(Instant.parse("2026-11-01T10:30:00Z")));

    @Test
    void loadsAnApprovedSurveyThatRunsAndIsReplannedAfterAnIncident() {
        demo.loadIfEmpty();

        PlanSnapshot survey = plan(ExpeditionStatus.APPROVED);
        List<Activity> activities = leaves(survey.itinerary());
        assertAll(
                () -> assertTrue(activities.size() >= 10),
                () -> assertTrue(activities.stream().map(Activity::requirements).distinct().count() >= 6),
                () -> assertTrue(activities.stream().anyMatch(a -> a.requirements().specialPermits().contains(PermitKind.NIGHT))),
                () -> assertTrue(survey.itinerary().stream().anyMatch(DemoDataTest::nestsAParallelBlock)),
                () -> assertEquals(List.of("CAPACITY"), survey.acceptedWarnings().stream().map(w -> w.issue().code()).toList()),
                () -> assertEquals(1, useCases.tracking().run(survey.id()).incidents().size()),
                () -> assertEquals(
                        List.of(ReplanProposal.Decision.PENDING),
                        useCases.proposalReview().of(survey.id(), ALL).items().stream().map(ProposalSnapshot::decision).toList()
                )
        );
    }

    @Test
    void loadsADraftWithACriticalError() {
        demo.loadIfEmpty();

        PlanSnapshot ridge = plan(ExpeditionStatus.DRAFT);

        assertTrue(useCases.review().validate(ridge.id()).hasCritical());
    }

    @Test
    void loadingAgainChangesNothing() {
        demo.loadIfEmpty();
        demo.loadIfEmpty();

        assertAll(
                () -> assertEquals(2, useCases.consult().all(ALL).totalItems()),
                () -> assertEquals(4, useCases.consultPersonnel().people(ALL).totalItems())
        );
    }

    private PlanSnapshot plan(ExpeditionStatus status) {
        return useCases.consult().all(ALL).items().stream()
                .filter(plan -> plan.status() == status)
                .findFirst()
                .orElseThrow();
    }

    private static List<Activity> leaves(List<ItineraryItem> items) {
        return items.stream().flatMap(item -> item.activities().stream()).toList();
    }

    private static boolean nestsAParallelBlock(ItineraryItem item) {
        Set<ActivityBlock.Arrangement> arrangements = item.blocks().stream()
                .map(ActivityBlock::arrangement)
                .collect(Collectors.toSet());
        return arrangements.equals(Set.of(ActivityBlock.Arrangement.SEQUENTIAL, ActivityBlock.Arrangement.PARALLEL));
    }
}
