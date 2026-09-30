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
        Activity sampling = Activity.sampling(certification())
                .named(id(), "sample")
                .estimated(Duration.ofHours(5), RiskLevel.LOW)
                .in(DELTA, window(Duration.ofHours(5)))
                .build();
        Activity transit = Activity.transit()
                .named(id(), "move")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, window(Duration.ofHours(2)))
                .build();
        Activity measurement = Activity.measurement(certification(), PROBE)
                .named(id(), "measure")
                .estimated(Duration.ofHours(3), RiskLevel.HIGH)
                .in(DELTA, window(Duration.ofHours(3)))
                .build();

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
        Activity shortSample = Activity.sampling(certification())
                .named(id(), "short")
                .estimated(Duration.ofHours(1), RiskLevel.LOW)
                .in(DELTA, window(Duration.ofHours(1)))
                .build();
        Activity longSample = Activity.sampling(certification())
                .named(id(), "long")
                .estimated(Duration.ofHours(6), RiskLevel.MEDIUM)
                .in(DELTA, window(Duration.ofHours(6)))
                .consuming(Map.of(vials, new Stock(4)))
                .build();

        assertAll(
                () -> assertEquals(Duration.ofHours(1), shortSample.estimatedDuration()),
                () -> assertEquals(Duration.ofHours(6), longSample.estimatedDuration()),
                () -> assertEquals(new Stock(4), longSample.estimatedConsumption().get(vials))
        );
    }

    @Test
    void nightActivityRaisesRiskAndCarriesNightRequirements() {
        CertificationId nightOperation = certification();
        Activity night = Activity.night(nightOperation)
                .named(id(), "watch")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, window(Duration.ofHours(2)))
                .build();
        Activity raised = Activity.night(nightOperation)
                .named(id(), "watch")
                .estimated(Duration.ofHours(2), RiskLevel.MEDIUM)
                .in(DELTA, window(Duration.ofHours(2)))
                .build();
        Activity alreadyHigh = Activity.night(nightOperation)
                .named(id(), "watch")
                .estimated(Duration.ofHours(2), RiskLevel.HIGH)
                .in(DELTA, window(Duration.ofHours(2)))
                .build();

        assertAll(
                () -> assertEquals(RiskLevel.MEDIUM, night.risk()),
                () -> assertEquals(RiskLevel.HIGH, raised.risk()),
                () -> assertEquals(RiskLevel.HIGH, alreadyHigh.risk()),
                () -> assertEquals(NightPermit.REQUIRED, night.requirements().nightPermit()),
                () -> assertEquals(InstrumentKind.LIGHTING, night.requirements().instrument().requiredKind().orElseThrow()),
                () -> assertEquals(Set.of(), night.requirements().certifications()),
                () -> assertEquals(Set.of(nightOperation), night.requirements().heldByEveryone())
        );
    }

    @Test
    void rejectsSelfAsPredecessor() {
        ActivityId activityId = id();

        assertThrows(InvalidItinerary.class, () -> Activity.transit()
                .named(activityId, "loop")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, window(Duration.ofHours(2)))
                .after(Set.of(activityId))
                .build());
    }

    @Test
    void diveRequiresDivingGearAndTheCertificationOfEveryDiver() {
        CertificationId diving = certification();

        Activity dive = Activity.dive(diving)
                .named(id(), "reef survey")
                .estimated(Duration.ofHours(2), RiskLevel.HIGH)
                .in(DELTA, window(Duration.ofHours(2)))
                .build();

        assertAll(
                () -> assertEquals(Set.of(diving), dive.requirements().heldByEveryone()),
                () -> assertEquals(Set.of(), dive.requirements().certifications()),
                () -> assertEquals(InstrumentKind.DIVING_GEAR, dive.requirements().instrument().requiredKind().orElseThrow()),
                () -> assertEquals(NightPermit.NONE, dive.requirements().nightPermit()),
                () -> assertEquals(RiskLevel.HIGH, dive.risk())
        );
    }

    @Test
    void campRequiresAVehicleAndCampGear() {
        Activity camp = Activity.camp()
                .named(id(), "base camp")
                .estimated(Duration.ofHours(3), RiskLevel.LOW)
                .in(DELTA, window(Duration.ofHours(3)))
                .build();

        assertAll(
                () -> assertEquals(VehicleRequirement.REQUIRED, camp.requirements().vehicle()),
                () -> assertEquals(InstrumentKind.CAMP_GEAR, camp.requirements().instrument().requiredKind().orElseThrow()),
                () -> assertEquals(Set.of(), camp.requirements().certifications()),
                () -> assertEquals(Set.of(), camp.requirements().heldByEveryone())
        );
    }

    @Test
    void anActivityNeedsAPlace() {
        Activity.Builder withoutPlace = Activity.transit()
                .named(id(), "move")
                .estimated(Duration.ofHours(2), RiskLevel.LOW);

        assertThrows(NullPointerException.class, withoutPlace::build);
    }

    @Test
    void rejectsWindowShorterThanEstimate() {
        assertThrows(InvalidItinerary.class, () -> Activity.transit()
                .named(id(), "short")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, window(Duration.ofHours(1)))
                .build());
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
