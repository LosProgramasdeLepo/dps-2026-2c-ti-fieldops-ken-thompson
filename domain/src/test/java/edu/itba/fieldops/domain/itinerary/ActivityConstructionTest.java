package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityConstructionTest {
    private static final Instant START = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final InstrumentKind PROBE = new InstrumentKind("probe");

    @Test
    void eachKindKeepsItsOwnDurationRiskAndRequirements() {
        Activity sampling = Activity.sampling(
                id(), "sample", Duration.ofHours(5), RiskLevel.LOW, window(Duration.ofHours(5)), Set.of(), DELTA, certification()
        );
        Activity transit = Activity.transit(
                id(), "move", Duration.ofHours(2), RiskLevel.LOW, window(Duration.ofHours(2)), Set.of(), DELTA
        );
        Activity measurement = Activity.measurement(
                id(), "measure", Duration.ofHours(3), RiskLevel.HIGH, window(Duration.ofHours(3)), Set.of(), DELTA, certification(), PROBE
        );

        assertAll(
                () -> assertEquals(VehicleRequirement.NONE, sampling.requirements().vehicle()),
                () -> assertEquals(VehicleRequirement.REQUIRED, transit.requirements().vehicle()),
                () -> assertInstanceOf(InstrumentRequirement.OfKind.class, measurement.requirements().instrument()),
                () -> assertEquals(Duration.ofHours(5), sampling.estimatedDuration()),
                () -> assertEquals(Duration.ofHours(2), transit.estimatedDuration()),
                () -> assertEquals(RiskLevel.HIGH, measurement.risk())
        );
    }

    @Test
    void twoSamplingsCanEstimateDifferentDurationsAndConsumption() {
        ConsumableId vials = new ConsumableId(UUID.randomUUID());
        Activity shortSample = Activity.sampling(
                id(), "short", Duration.ofHours(1), RiskLevel.LOW, window(Duration.ofHours(1)), Set.of(), DELTA, certification()
        );
        Activity longSample = Activity.sampling(
                id(),
                "long",
                Duration.ofHours(6),
                RiskLevel.MEDIUM,
                window(Duration.ofHours(6)),
                Set.of(),
                DELTA,
                certification(),
                Map.of(vials, new Stock(4))
        );

        assertAll(
                () -> assertEquals(Duration.ofHours(1), shortSample.estimatedDuration()),
                () -> assertEquals(Duration.ofHours(6), longSample.estimatedDuration()),
                () -> assertEquals(new Stock(4), longSample.requirements().estimatedConsumption().get(vials))
        );
    }

    @Test
    void nightActivityRaisesRiskAndCarriesNightRequirements() {
        CertificationId nightOperation = certification();
        InstrumentKind lighting = new InstrumentKind("lighting");
        Activity night = Activity.night(
                id(), "watch", Duration.ofHours(2), RiskLevel.LOW, window(Duration.ofHours(2)), Set.of(), DELTA, nightOperation, lighting
        );
        Activity raised = Activity.night(
                id(), "watch", Duration.ofHours(2), RiskLevel.MEDIUM, window(Duration.ofHours(2)), Set.of(), DELTA, nightOperation, lighting
        );
        Activity alreadyHigh = Activity.night(
                id(), "watch", Duration.ofHours(2), RiskLevel.HIGH, window(Duration.ofHours(2)), Set.of(), DELTA, nightOperation, lighting
        );

        assertAll(
                () -> assertEquals(RiskLevel.MEDIUM, night.risk()),
                () -> assertEquals(RiskLevel.HIGH, raised.risk()),
                () -> assertEquals(RiskLevel.HIGH, alreadyHigh.risk()),
                () -> assertEquals(NightPermit.REQUIRED, night.requirements().nightPermit()),
                () -> assertEquals(lighting, night.requirements().instrument().requiredKind().orElseThrow()),
                () -> assertEquals(Set.of(), night.requirements().certifications()),
                () -> assertEquals(Set.of(nightOperation), night.requirements().heldByEveryone())
        );
    }

    @Test
    void rejectsSelfAsPredecessor() {
        ActivityId activityId = id();

        assertThrows(InvalidItinerary.class, () -> Activity.transit(
                activityId,
                "loop",
                Duration.ofHours(2),
                RiskLevel.LOW,
                window(Duration.ofHours(2)),
                Set.of(activityId),
                DELTA
        ));
    }

    @Test
    void rejectsWindowShorterThanEstimate() {
        assertThrows(InvalidItinerary.class, () -> Activity.transit(
                id(),
                "short",
                Duration.ofHours(2),
                RiskLevel.LOW,
                window(Duration.ofHours(1)),
                Set.of(),
                DELTA
        ));
    }

    private static ActivityId id() {
        return new ActivityId(UUID.randomUUID());
    }

    private static CertificationId certification() {
        return new CertificationId(UUID.randomUUID());
    }

    private static TimePeriod window(Duration length) {
        return new TimePeriod(START, START.plus(length));
    }
}
