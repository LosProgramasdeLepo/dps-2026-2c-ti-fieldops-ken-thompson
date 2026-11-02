package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.usecase.catalog.ConsultPersonnel;
import edu.itba.fieldops.domain.usecase.expedition.ConsultExpedition;
import edu.itba.fieldops.domain.usecase.expedition.PlanSnapshot;
import edu.itba.fieldops.domain.usecase.expedition.ProposalSnapshot;
import edu.itba.fieldops.domain.usecase.expedition.ReviewExpedition;
import edu.itba.fieldops.domain.usecase.expedition.ReviewReplanProposal;
import edu.itba.fieldops.domain.usecase.expedition.TrackExpedition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles({PersistenceProfiles.MEMORY, DemoData.PROFILE})
@Import(DemoDataTest.FixedTime.class)
class DemoDataTest {
    private static final PageRequest ALL = new PageRequest(0, 100);

    @Autowired
    private DemoData demo;
    @Autowired
    private ConsultExpedition consult;
    @Autowired
    private ConsultPersonnel consultPersonnel;
    @Autowired
    private ReviewExpedition review;
    @Autowired
    private TrackExpedition tracking;
    @Autowired
    private ReviewReplanProposal proposalReview;

    @Test
    void loadsAnApprovedSurveyThatRunsAndIsReplannedAfterAnIncident() {
        PlanSnapshot survey = plan(ExpeditionStatus.APPROVED);
        List<Activity> activities = leaves(survey.itinerary());
        assertAll(
                () -> assertTrue(activities.size() >= 10),
                () -> assertTrue(activities.stream().map(Activity::requirements).distinct().count() >= 6),
                () -> assertTrue(activities.stream().anyMatch(a -> a.requirements().specialPermits().contains(PermitKind.NIGHT))),
                () -> assertTrue(survey.itinerary().stream().anyMatch(DemoDataTest::nestsAParallelBlock)),
                () -> assertEquals(List.of("CAPACITY"), survey.acceptedWarnings().stream().map(w -> w.issue().code()).toList()),
                () -> assertEquals(1, tracking.run(survey.id()).incidents().size()),
                () -> assertEquals(
                        List.of(ReplanProposal.Decision.PENDING),
                        proposalReview.of(survey.id(), ALL).items().stream().map(ProposalSnapshot::decision).toList()
                )
        );
    }

    @Test
    void loadsADraftWithACriticalError() {
        PlanSnapshot ridge = plan(ExpeditionStatus.DRAFT);

        assertTrue(review.validate(ridge.id()).hasCritical());
    }

    @Test
    void loadingAgainChangesNothing() {
        demo.loadIfEmpty();

        assertAll(
                () -> assertEquals(2, consult.all(ALL).totalItems()),
                () -> assertEquals(4, consultPersonnel.people(ALL).totalItems())
        );
    }

    private PlanSnapshot plan(ExpeditionStatus status) {
        return consult.all(ALL).items().stream()
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

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTime {
        @Bean
        @Primary
        FixedClock clock() {
            return new FixedClock(Instant.parse("2026-11-01T10:30:00Z"));
        }
    }
}
