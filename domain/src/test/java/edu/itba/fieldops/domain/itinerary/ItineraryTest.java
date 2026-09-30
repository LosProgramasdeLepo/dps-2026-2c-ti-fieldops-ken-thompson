package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItineraryTest {
    private static final Instant START = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Test
    void sequentialDurationIsTheSumAndParallelDurationIsTheMax() {
        Activity first = transit("approach", 0, 2);
        Activity left = sampling("left", 2, 6, RiskLevel.MEDIUM);
        Activity right = sampling("right", 2, 5, RiskLevel.HIGH);
        Activity last = transit("return", 6, 7);
        Itinerary itinerary = new Itinerary();
        itinerary.add(ActivityBlock.sequential(first, ActivityBlock.parallel(left, right), last));

        assertAll(
                () -> assertEquals(Duration.ofHours(7), itinerary.estimatedDuration()),
                () -> assertEquals(RiskLevel.HIGH, itinerary.risk()),
                () -> assertEquals(4, itinerary.activities().size())
        );
    }

    @Test
    void parallelConsumptionIsTheSumOfItsParts() {
        ConsumableId fuel = new ConsumableId(UUID.randomUUID());
        Activity left = transit("left", 0, 2, Map.of(fuel, new Stock(3)));
        Activity right = transit("right", 0, 5, Map.of(fuel, new Stock(4)));
        Itinerary itinerary = new Itinerary();
        itinerary.add(ActivityBlock.parallel(left, right));

        assertAll(
                () -> assertEquals(Duration.ofHours(5), itinerary.estimatedDuration()),
                () -> assertEquals(new Stock(7), itinerary.estimatedConsumption().get(fuel))
        );
    }

    @Test
    void removingTheLastLeafDropsTheBlock() {
        Activity left = transit("left", 0, 2);
        Activity right = transit("right", 0, 2);
        Itinerary itinerary = new Itinerary();
        itinerary.add(ActivityBlock.parallel(left, right));

        itinerary.remove(left.id());
        assertEquals(List.of(right), itinerary.items());

        itinerary.remove(right.id());
        assertEquals(List.of(), itinerary.activities());
    }

    @Test
    void rejectsABlockThatRepeatsAnActivity() {
        Activity activity = transit("ride", 0, 2);
        assertThrows(InvalidItinerary.class, () -> ActivityBlock.parallel(activity, activity));
    }

    @Test
    void rejectsABlockThatDependsOnAnUnknownActivity() {
        Activity first = Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "first")
                .estimated(Duration.ofHours(1), RiskLevel.LOW)
                .in(DELTA, window(0, 1))
                .after(Set.of(new ActivityId(UUID.randomUUID())))
                .build();
        Activity second = Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "second")
                .estimated(Duration.ofHours(1), RiskLevel.LOW)
                .in(DELTA, window(2, 3))
                .after(Set.of(first.id()))
                .build();
        Itinerary itinerary = new Itinerary();

        assertThrows(InvalidItinerary.class, () -> itinerary.add(ActivityBlock.parallel(second, first)));
        assertEquals(List.of(), itinerary.activities());
    }

    @Test
    void rejectsASequenceWhosePartsOverlap() {
        Activity first = transit("approach", 0, 2);
        Activity second = transit("return", 1, 3);
        Itinerary itinerary = new Itinerary();

        assertThrows(InvalidItinerary.class, () -> itinerary.add(ActivityBlock.sequential(first, second)));
        assertEquals(List.of(), itinerary.activities());
    }

    @Test
    void eachPartOfASequenceFollowsEveryLeafOfThePreviousPart() {
        Activity approach = transit("approach", 0, 2);
        Activity left = transit("left", 2, 4);
        Activity right = transit("right", 2, 5);
        Activity back = transit("return", 5, 6);
        Itinerary itinerary = new Itinerary();
        itinerary.add(ActivityBlock.sequential(approach, ActivityBlock.parallel(left, right), back));

        assertAll(
                () -> assertEquals(Set.of(), itinerary.predecessorsOf(approach.id())),
                () -> assertEquals(Set.of(approach.id()), itinerary.predecessorsOf(left.id())),
                () -> assertEquals(Set.of(approach.id()), itinerary.predecessorsOf(right.id())),
                () -> assertEquals(Set.of(left.id(), right.id()), itinerary.predecessorsOf(back.id()))
        );
    }

    @Test
    void delayingAPartOfASequencePushesTheNextParts() {
        Activity first = transit("approach", 0, 2);
        Activity second = transit("return", 2, 3);
        Itinerary itinerary = new Itinerary();
        itinerary.add(ActivityBlock.sequential(first, second));

        itinerary.delay(first.id(), Duration.ofHours(1));

        assertAll(
                () -> assertEquals(window(1, 3), itinerary.activityOf(first.id()).window()),
                () -> assertEquals(window(3, 4), itinerary.activityOf(second.id()).window())
        );
    }

    @Test
    void rejectsADependencyThatContradictsASequence() {
        Activity first = transit("approach", 0, 2);
        Activity second = transit("return", 2, 3);
        Itinerary itinerary = new Itinerary();
        itinerary.add(ActivityBlock.sequential(first, second));

        InvalidItinerary rejected = assertThrows(InvalidItinerary.class, () -> itinerary.addDependency(first.id(), second.id()));

        assertEquals("activity dependencies form a cycle", rejected.getMessage());
    }

    @Test
    void theRootDoesNotOrderItsItems() {
        Activity first = transit("approach", 0, 2);
        Activity second = transit("survey", 0, 2);
        Itinerary itinerary = new Itinerary();
        itinerary.add(first);

        itinerary.add(second);

        assertEquals(Set.of(), itinerary.predecessorsOf(second.id()));
    }

    private static Activity transit(String name, int fromHour, int toHour) {
        return transit(name, fromHour, toHour, Map.of());
    }

    private static Activity transit(String name, int fromHour, int toHour, Map<ConsumableId, Stock> consumption) {
        return Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), name)
                .estimated(Duration.ofHours(toHour - fromHour), RiskLevel.LOW)
                .in(DELTA, window(fromHour, toHour))
                .consuming(consumption)
                .build();
    }

    private static Activity sampling(String name, int fromHour, int toHour, RiskLevel risk) {
        return Activity.sampling(new CertificationId(UUID.randomUUID()))
                .named(new ActivityId(UUID.randomUUID()), name)
                .estimated(Duration.ofHours(toHour - fromHour), risk)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(START.plus(Duration.ofHours(fromHour)), START.plus(Duration.ofHours(toHour)));
    }
}
