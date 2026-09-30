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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ActivityTest {
    private static final Instant START = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final InstrumentKind PROBE = new InstrumentKind("probe");
    private static final CertificationId SAMPLING = certification();
    private static final CertificationId NIGHT_OPERATION = certification();
    private static final CertificationId DIVING = certification();

    @ParameterizedTest(name = "{0}")
    @MethodSource("requirementsOfEachKind")
    void eachKindDeclaresItsRequirements(String kind, Activity.Builder builder, ResourceRequirements expected) {
        assertEquals(expected, planned(builder, RiskLevel.MEDIUM).requirements());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("riskOfEachKind")
    void onlyTheNightKindRaisesTheEstimatedRisk(String kind, Activity.Builder builder, RiskLevel expected) {
        assertEquals(expected, planned(builder, RiskLevel.MEDIUM).risk());
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

    private static Stream<Arguments> requirementsOfEachKind() {
        InstrumentRequirement none = new InstrumentRequirement.None();
        return Stream.of(
                arguments("sampling", Activity.sampling(SAMPLING), new ResourceRequirements(
                        Set.of(SAMPLING), Set.of(), VehicleRequirement.NONE, none, NightPermit.NONE
                )),
                arguments("measurement", Activity.measurement(SAMPLING, PROBE), new ResourceRequirements(
                        Set.of(SAMPLING), Set.of(), VehicleRequirement.NONE, new InstrumentRequirement.OfKind(PROBE), NightPermit.NONE
                )),
                arguments("transit", Activity.transit(), new ResourceRequirements(
                        Set.of(), Set.of(), VehicleRequirement.REQUIRED, none, NightPermit.NONE
                )),
                arguments("night", Activity.night(NIGHT_OPERATION), new ResourceRequirements(
                        Set.of(), Set.of(NIGHT_OPERATION), VehicleRequirement.NONE,
                        new InstrumentRequirement.OfKind(InstrumentKind.LIGHTING), NightPermit.REQUIRED
                )),
                arguments("dive", Activity.dive(DIVING), new ResourceRequirements(
                        Set.of(), Set.of(DIVING), VehicleRequirement.NONE,
                        new InstrumentRequirement.OfKind(InstrumentKind.DIVING_GEAR), NightPermit.NONE
                )),
                arguments("camp", Activity.camp(), new ResourceRequirements(
                        Set.of(), Set.of(), VehicleRequirement.REQUIRED,
                        new InstrumentRequirement.OfKind(InstrumentKind.CAMP_GEAR), NightPermit.NONE
                ))
        );
    }

    private static Stream<Arguments> riskOfEachKind() {
        return Stream.of(
                arguments("sampling", Activity.sampling(SAMPLING), RiskLevel.MEDIUM),
                arguments("measurement", Activity.measurement(SAMPLING, PROBE), RiskLevel.MEDIUM),
                arguments("transit", Activity.transit(), RiskLevel.MEDIUM),
                arguments("night", Activity.night(NIGHT_OPERATION), RiskLevel.HIGH),
                arguments("dive", Activity.dive(DIVING), RiskLevel.MEDIUM),
                arguments("camp", Activity.camp(), RiskLevel.MEDIUM)
        );
    }

    private static Activity planned(Activity.Builder builder, RiskLevel risk) {
        return builder.named(id(), "planned")
                .estimated(Duration.ofHours(2), risk)
                .in(DELTA, window(Duration.ofHours(2)))
                .build();
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
