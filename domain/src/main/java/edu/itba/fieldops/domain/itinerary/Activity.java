package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.Texts;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Activity {
    private final ActivityId id;
    private final String name;
    private final Duration estimatedDuration;
    private final RiskLevel risk;
    private final ResourceRequirements requirements;
    private final TimePeriod window;
    private final Set<ActivityId> predecessors;
    private final WorkZone zone;

    private Activity(
            ActivityId id,
            String name,
            Duration estimatedDuration,
            RiskLevel risk,
            ResourceRequirements requirements,
            TimePeriod window,
            Set<ActivityId> predecessors,
            WorkZone zone
    ) {
        this.id = Objects.requireNonNull(id, "activity id");
        this.name = Texts.required(name, "activity name");
        this.estimatedDuration = Objects.requireNonNull(estimatedDuration, "estimated duration");
        this.risk = Objects.requireNonNull(risk, "risk");
        this.requirements = Objects.requireNonNull(requirements, "requirements");
        this.window = Objects.requireNonNull(window, "activity window");
        this.predecessors = Set.copyOf(predecessors);
        this.zone = Objects.requireNonNull(zone, "activity zone");
        if (estimatedDuration.isNegative() || estimatedDuration.isZero()) {
            throw new InvalidItinerary("estimated duration must be positive");
        }
        if (predecessors.contains(id)) {
            throw new InvalidItinerary("activity cannot precede itself");
        }
        if (Duration.between(window.start(), window.end()).compareTo(estimatedDuration) < 0) {
            throw new InvalidItinerary("activity window is shorter than estimated duration");
        }
    }

    public static Activity sampling(
            ActivityId id,
            String name,
            Duration estimatedDuration,
            RiskLevel risk,
            TimePeriod window,
            Set<ActivityId> predecessors,
            WorkZone zone,
            CertificationId certification
    ) {
        return sampling(id, name, estimatedDuration, risk, window, predecessors, zone, certification, Map.of());
    }

    public static Activity sampling(
            ActivityId id,
            String name,
            Duration estimatedDuration,
            RiskLevel risk,
            TimePeriod window,
            Set<ActivityId> predecessors,
            WorkZone zone,
            CertificationId certification,
            Map<ConsumableId, Stock> estimatedConsumption
    ) {
        Objects.requireNonNull(certification, "sampling certification");
        return new Activity(
                id,
                name,
                estimatedDuration,
                risk,
                new ResourceRequirements(
                        Set.of(certification),
                        Set.of(),
                        VehicleRequirement.NONE,
                        new InstrumentRequirement.None(),
                        NightPermit.NONE,
                        estimatedConsumption
                ),
                window,
                predecessors,
                zone
        );
    }

    public static Activity measurement(
            ActivityId id,
            String name,
            Duration estimatedDuration,
            RiskLevel risk,
            TimePeriod window,
            Set<ActivityId> predecessors,
            WorkZone zone,
            CertificationId certification,
            InstrumentKind instrument
    ) {
        return measurement(id, name, estimatedDuration, risk, window, predecessors, zone, certification, instrument, Map.of());
    }

    public static Activity measurement(
            ActivityId id,
            String name,
            Duration estimatedDuration,
            RiskLevel risk,
            TimePeriod window,
            Set<ActivityId> predecessors,
            WorkZone zone,
            CertificationId certification,
            InstrumentKind instrument,
            Map<ConsumableId, Stock> estimatedConsumption
    ) {
        Objects.requireNonNull(certification, "operator certification");
        return new Activity(
                id,
                name,
                estimatedDuration,
                risk,
                new ResourceRequirements(
                        Set.of(certification),
                        Set.of(),
                        VehicleRequirement.NONE,
                        new InstrumentRequirement.OfKind(instrument),
                        NightPermit.NONE,
                        estimatedConsumption
                ),
                window,
                predecessors,
                zone
        );
    }

    public static Activity transit(
            ActivityId id,
            String name,
            Duration estimatedDuration,
            RiskLevel risk,
            TimePeriod window,
            Set<ActivityId> predecessors,
            WorkZone zone
    ) {
        return transit(id, name, estimatedDuration, risk, window, predecessors, zone, Map.of());
    }

    public static Activity transit(
            ActivityId id,
            String name,
            Duration estimatedDuration,
            RiskLevel risk,
            TimePeriod window,
            Set<ActivityId> predecessors,
            WorkZone zone,
            Map<ConsumableId, Stock> estimatedConsumption
    ) {
        return new Activity(
                id,
                name,
                estimatedDuration,
                risk,
                new ResourceRequirements(
                        Set.of(),
                        Set.of(),
                        VehicleRequirement.REQUIRED,
                        new InstrumentRequirement.None(),
                        NightPermit.NONE,
                        estimatedConsumption
                ),
                window,
                predecessors,
                zone
        );
    }

    public static Activity night(
            ActivityId id,
            String name,
            Duration estimatedDuration,
            RiskLevel risk,
            TimePeriod window,
            Set<ActivityId> predecessors,
            WorkZone zone,
            CertificationId nightOperation,
            InstrumentKind lighting
    ) {
        return night(id, name, estimatedDuration, risk, window, predecessors, zone, nightOperation, lighting, Map.of());
    }

    public static Activity night(
            ActivityId id,
            String name,
            Duration estimatedDuration,
            RiskLevel risk,
            TimePeriod window,
            Set<ActivityId> predecessors,
            WorkZone zone,
            CertificationId nightOperation,
            InstrumentKind lighting,
            Map<ConsumableId, Stock> estimatedConsumption
    ) {
        Objects.requireNonNull(risk, "risk");
        Objects.requireNonNull(nightOperation, "night certification");
        Objects.requireNonNull(lighting, "lighting");
        return new Activity(
                id,
                name,
                estimatedDuration,
                risk.raised(),
                new ResourceRequirements(
                        Set.of(),
                        Set.of(nightOperation),
                        VehicleRequirement.NONE,
                        new InstrumentRequirement.OfKind(lighting),
                        NightPermit.REQUIRED,
                        estimatedConsumption
                ),
                window,
                predecessors,
                zone
        );
    }

    public ActivityId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Duration estimatedDuration() {
        return estimatedDuration;
    }

    public RiskLevel risk() {
        return risk;
    }

    public ResourceRequirements requirements() {
        return requirements;
    }

    public TimePeriod window() {
        return window;
    }

    public Set<ActivityId> predecessors() {
        return predecessors;
    }

    public WorkZone zone() {
        return zone;
    }

    Activity withWindow(TimePeriod window) {
        return new Activity(id, name, estimatedDuration, risk, requirements, window, predecessors, zone);
    }

    Activity withPredecessor(ActivityId predecessorId) {
        Objects.requireNonNull(predecessorId, "predecessor id");
        Set<ActivityId> next = new HashSet<>(predecessors);
        next.add(predecessorId);
        return new Activity(id, name, estimatedDuration, risk, requirements, window, next, zone);
    }

    Activity withoutPredecessor(ActivityId predecessorId) {
        Objects.requireNonNull(predecessorId, "predecessor id");
        Set<ActivityId> next = new HashSet<>(predecessors);
        if (!next.remove(predecessorId)) {
            throw new InvalidItinerary("unknown predecessor: " + predecessorId);
        }
        return new Activity(id, name, estimatedDuration, risk, requirements, window, next, zone);
    }
}
