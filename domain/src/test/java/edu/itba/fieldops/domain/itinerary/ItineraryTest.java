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
        Activity first = transit("approach", 2);
        Activity left = sampling("left", 4, RiskLevel.MEDIUM);
        Activity right = sampling("right", 3, RiskLevel.HIGH);
        Activity last = transit("return", 1);
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
        Activity left = transit("left", 2, Map.of(fuel, new Stock(3)));
        Activity right = transit("right", 5, Map.of(fuel, new Stock(4)));
        Itinerary itinerary = new Itinerary();
        itinerary.add(ActivityBlock.parallel(left, right));

        assertAll(
                () -> assertEquals(Duration.ofHours(5), itinerary.estimatedDuration()),
                () -> assertEquals(new Stock(7), itinerary.estimatedConsumption().get(fuel))
        );
    }

    @Test
    void removingTheLastLeafDropsTheBlock() {
        Activity left = transit("left", 2);
        Activity right = transit("right", 2);
        Itinerary itinerary = new Itinerary();
        itinerary.add(ActivityBlock.parallel(left, right));

        itinerary.remove(left.id());
        assertEquals(List.of(right), itinerary.items());

        itinerary.remove(right.id());
        assertEquals(List.of(), itinerary.activities());
    }

    @Test
    void rejectsABlockThatRepeatsAnActivity() {
        Activity activity = transit("ride", 2);
        assertThrows(InvalidItinerary.class, () -> ActivityBlock.parallel(activity, activity));
    }

    private static Activity transit(String name, int hours) {
        return transit(name, hours, Map.of());
    }

    private static Activity transit(String name, int hours, Map<ConsumableId, Stock> consumption) {
        return Activity.transit(
                new ActivityId(UUID.randomUUID()),
                name,
                Duration.ofHours(hours),
                RiskLevel.LOW,
                window(hours),
                Set.of(),
                DELTA,
                consumption
        );
    }

    private static Activity sampling(String name, int hours, RiskLevel risk) {
        return Activity.sampling(
                new ActivityId(UUID.randomUUID()),
                name,
                Duration.ofHours(hours),
                risk,
                window(hours),
                Set.of(),
                DELTA,
                new CertificationId(UUID.randomUUID())
        );
    }

    private static TimePeriod window(int hours) {
        return new TimePeriod(START, START.plus(Duration.ofHours(hours)));
    }
}
