package edu.itba.fieldops.adapters.jpa.tracking;

import edu.itba.fieldops.domain.expedition.ExecutionState;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "expedition_runs")
public class RunEntity {
    @Id
    @Column(name = "expedition_id")
    private UUID expeditionId;

    @Column(nullable = false)
    private String status;

    @ElementCollection
    @CollectionTable(name = "run_activities", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    private List<StoredActivityRun> activities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "run_incidents", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    private List<StoredIncident> incidents = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "run_observations", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "position")
    private List<StoredObservation> observations = new ArrayList<>();

    protected RunEntity() {
    }

    RunEntity(ExecutionState state) {
        this.expeditionId = state.expeditionId().value();
        this.status = state.status().name();
        this.activities = new ArrayList<>(state.activities().stream().map(StoredActivityRun::of).toList());
        this.incidents = new ArrayList<>(state.incidents().stream().map(StoredIncident::of).toList());
        this.observations = new ArrayList<>(state.observations().stream().map(StoredObservation::of).toList());
    }

    public ExpeditionExecution toDomain() {
        return ExpeditionExecution.restore(new ExecutionState(
                new ExpeditionId(expeditionId),
                ExpeditionExecution.Status.valueOf(status),
                activities.stream().map(StoredActivityRun::toDomain).toList(),
                incidents.stream().map(StoredIncident::toDomain).toList(),
                observations.stream().map(StoredObservation::toDomain).toList()
        ));
    }
}
