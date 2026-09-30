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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class Activity implements ItineraryItem {
    private final ActivityId id;
    private final String name;
    private final Duration estimatedDuration;
    private final RiskLevel risk;
    private final Map<ConsumableId, Stock> estimatedConsumption;
    private final ResourceRequirements requirements;
    private final WorkZone zone;
    private final TimePeriod window;
    private final Set<ActivityId> predecessors;

    private Activity(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "activity id");
        this.name = Texts.required(builder.name, "activity name");
        this.estimatedDuration = Objects.requireNonNull(builder.estimatedDuration, "estimated duration");
        this.risk = builder.riskFactor.apply(Objects.requireNonNull(builder.risk, "risk"));
        this.estimatedConsumption = Map.copyOf(builder.estimatedConsumption);
        this.requirements = builder.requirements;
        this.zone = Objects.requireNonNull(builder.zone, "activity zone");
        this.window = Objects.requireNonNull(builder.window, "activity window");
        this.predecessors = Set.copyOf(builder.predecessors);
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

    public static Builder sampling(CertificationId certification) {
        return new Builder(new ResourceRequirements(
                Set.of(Objects.requireNonNull(certification, "sampling certification")),
                Set.of(),
                VehicleRequirement.NONE,
                new InstrumentRequirement.None(),
                NightPermit.NONE
        ));
    }

    public static Builder measurement(CertificationId certification, InstrumentKind instrument) {
        return new Builder(new ResourceRequirements(
                Set.of(Objects.requireNonNull(certification, "operator certification")),
                Set.of(),
                VehicleRequirement.NONE,
                new InstrumentRequirement.OfKind(instrument),
                NightPermit.NONE
        ));
    }

    public static Builder transit() {
        return new Builder(new ResourceRequirements(
                Set.of(),
                Set.of(),
                VehicleRequirement.REQUIRED,
                new InstrumentRequirement.None(),
                NightPermit.NONE
        ));
    }

    public static Builder night(CertificationId nightOperation) {
        return new Builder(new ResourceRequirements(
                Set.of(),
                Set.of(Objects.requireNonNull(nightOperation, "night certification")),
                VehicleRequirement.NONE,
                new InstrumentRequirement.OfKind(InstrumentKind.LIGHTING),
                NightPermit.REQUIRED
        ), RiskLevel::raised);
    }

    public static Builder dive(CertificationId diving) {
        return new Builder(new ResourceRequirements(
                Set.of(),
                Set.of(Objects.requireNonNull(diving, "diving certification")),
                VehicleRequirement.NONE,
                new InstrumentRequirement.OfKind(InstrumentKind.DIVING_GEAR),
                NightPermit.NONE
        ));
    }

    public static Builder camp() {
        return new Builder(new ResourceRequirements(
                Set.of(),
                Set.of(),
                VehicleRequirement.REQUIRED,
                new InstrumentRequirement.OfKind(InstrumentKind.CAMP_GEAR),
                NightPermit.NONE
        ));
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

    public Map<ConsumableId, Stock> estimatedConsumption() {
        return estimatedConsumption;
    }

    public ResourceRequirements requirements() {
        return requirements;
    }

    public WorkZone zone() {
        return zone;
    }

    public TimePeriod window() {
        return window;
    }

    public Set<ActivityId> predecessors() {
        return predecessors;
    }

    @Override
    public Duration duration(Function<Activity, Duration> leafDuration) {
        return leafDuration.apply(this);
    }

    @Override
    public List<Activity> activities() {
        return List.of(this);
    }

    Activity withWindow(TimePeriod window) {
        return new Builder(this).in(zone, window).build();
    }

    Activity withPredecessor(ActivityId predecessorId) {
        Objects.requireNonNull(predecessorId, "predecessor id");
        Set<ActivityId> next = new HashSet<>(predecessors);
        next.add(predecessorId);
        return new Builder(this).after(next).build();
    }

    Activity withoutPredecessor(ActivityId predecessorId) {
        Objects.requireNonNull(predecessorId, "predecessor id");
        Set<ActivityId> next = new HashSet<>(predecessors);
        if (!next.remove(predecessorId)) {
            throw new InvalidItinerary("unknown predecessor: " + predecessorId);
        }
        return new Builder(this).after(next).build();
    }

    public static final class Builder {
        private final ResourceRequirements requirements;
        private final UnaryOperator<RiskLevel> riskFactor;
        private ActivityId id;
        private String name;
        private Duration estimatedDuration;
        private RiskLevel risk;
        private Map<ConsumableId, Stock> estimatedConsumption = Map.of();
        private WorkZone zone;
        private TimePeriod window;
        private Set<ActivityId> predecessors = Set.of();

        private Builder(ResourceRequirements requirements) {
            this(requirements, UnaryOperator.identity());
        }

        private Builder(ResourceRequirements requirements, UnaryOperator<RiskLevel> riskFactor) {
            this.requirements = requirements;
            this.riskFactor = riskFactor;
        }

        private Builder(Activity source) {
            this(source.requirements);
            this.id = source.id;
            this.name = source.name;
            this.estimatedDuration = source.estimatedDuration;
            this.risk = source.risk;
            this.estimatedConsumption = source.estimatedConsumption;
            this.zone = source.zone;
            this.window = source.window;
            this.predecessors = source.predecessors;
        }

        public Builder named(ActivityId id, String name) {
            this.id = id;
            this.name = name;
            return this;
        }

        public Builder estimated(Duration estimatedDuration, RiskLevel risk) {
            this.estimatedDuration = estimatedDuration;
            this.risk = risk;
            return this;
        }

        public Builder consuming(Map<ConsumableId, Stock> estimatedConsumption) {
            this.estimatedConsumption = Objects.requireNonNull(estimatedConsumption, "estimated consumption");
            return this;
        }

        public Builder in(WorkZone zone, TimePeriod window) {
            this.zone = zone;
            this.window = window;
            return this;
        }

        public Builder after(Set<ActivityId> predecessors) {
            this.predecessors = Objects.requireNonNull(predecessors, "predecessors");
            return this;
        }

        public Activity build() {
            return new Activity(this);
        }
    }
}
