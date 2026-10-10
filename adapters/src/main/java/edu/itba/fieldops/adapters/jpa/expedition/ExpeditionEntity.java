package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionState;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "expeditions")
public class ExpeditionEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private int version;

    @Column(name = "supersedes_id")
    private UUID supersedesId;

    @Column(nullable = false)
    private String status;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @ElementCollection
    @CollectionTable(name = "expedition_objectives", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    @Column(name = "description", nullable = false)
    private List<String> objectives = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "expedition_zones", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    @Column(name = "name", nullable = false)
    private List<String> zones = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "expedition_responsibles", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    @Column(name = "person_id", nullable = false)
    private List<UUID> responsibles = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "expedition_restrictions", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    @Column(name = "description", nullable = false)
    private List<String> restrictions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "expedition_permits", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    @Column(name = "permit_id", nullable = false)
    private List<UUID> permits = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "accepted_warnings", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    private List<StoredWarning> acceptedWarnings = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "itinerary_nodes", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    private List<StoredNode> nodes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "activities", joinColumns = @JoinColumn(name = "expedition_id"))
    private Set<StoredActivity> activities = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "activity_certifications", joinColumns = @JoinColumn(name = "expedition_id"))
    private Set<StoredRequirement> certifications = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "activity_instrument_kinds", joinColumns = @JoinColumn(name = "expedition_id"))
    private Set<StoredKind> instrumentKinds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "activity_permit_kinds", joinColumns = @JoinColumn(name = "expedition_id"))
    private Set<StoredKind> permitKinds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "activity_consumption", joinColumns = @JoinColumn(name = "expedition_id"))
    private Set<StoredConsumption> consumption = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "activity_predecessors", joinColumns = @JoinColumn(name = "expedition_id"))
    private Set<StoredPredecessor> predecessors = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "person_assignments", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    @AttributeOverride(name = "resourceId", column = @Column(name = "person_id", nullable = false))
    private List<StoredBooking> people = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "vehicle_assignments", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    @AttributeOverride(name = "resourceId", column = @Column(name = "vehicle_id", nullable = false))
    private List<StoredBooking> vehicles = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "instrument_assignments", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    @AttributeOverride(name = "resourceId", column = @Column(name = "instrument_id", nullable = false))
    private List<StoredBooking> instruments = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "consumable_assignments", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    private List<StoredConsumableAssignment> consumables = new ArrayList<>();

    protected ExpeditionEntity() {
    }

    ExpeditionEntity(ExpeditionState state) {
        ExpeditionCharter charter = state.charter();
        ItineraryRows itinerary = ItineraryRows.of(state.items());
        AssignmentRows assignments = AssignmentRows.of(state.assignments());
        this.id = state.id().value();
        this.version = state.version();
        this.supersedesId = state.supersedes().map(ExpeditionId::value).orElse(null);
        this.status = state.status().name();
        this.periodStart = charter.period().start();
        this.periodEnd = charter.period().end();
        this.objectives = new ArrayList<>(charter.objectives().stream().map(Objective::text).toList());
        this.zones = new ArrayList<>(charter.zones().stream().map(WorkZone::name).toList());
        this.responsibles = new ArrayList<>(charter.responsibles().stream().map(PersonId::value).toList());
        this.restrictions = new ArrayList<>(charter.restrictions().stream().map(Restriction::text).toList());
        this.permits = new ArrayList<>(state.permits().stream().map(PermitId::value).toList());
        this.acceptedWarnings = new ArrayList<>(state.acceptedWarnings().stream().map(StoredWarning::of).toList());
        this.nodes = itinerary.nodes();
        this.activities = itinerary.activities();
        this.certifications = itinerary.certifications();
        this.instrumentKinds = itinerary.instrumentKinds();
        this.permitKinds = itinerary.permitKinds();
        this.consumption = itinerary.consumption();
        this.predecessors = itinerary.predecessors();
        this.people = assignments.people();
        this.vehicles = assignments.vehicles();
        this.instruments = assignments.instruments();
        this.consumables = assignments.consumables();
    }

    public Expedition toDomain() {
        return Expedition.restore(new ExpeditionState(
                new ExpeditionId(id),
                version,
                Optional.ofNullable(supersedesId).map(ExpeditionId::new),
                ExpeditionStatus.valueOf(status),
                new ExpeditionCharter(
                        objectives.stream().map(Objective::new).toList(),
                        new TimePeriod(periodStart, periodEnd),
                        zones.stream().map(WorkZone::new).toList(),
                        responsibles.stream().map(PersonId::new).toList(),
                        restrictions.stream().map(Restriction::new).toList()
                ),
                new ItineraryRows(
                        nodes, activities, certifications, instrumentKinds, permitKinds, consumption, predecessors
                ).items(),
                new AssignmentRows(people, vehicles, instruments, consumables).assignments(),
                permits.stream().map(PermitId::new).toList(),
                acceptedWarnings.stream().map(StoredWarning::toDomain).toList()
        ));
    }

    UUID id() {
        return id;
    }
}
