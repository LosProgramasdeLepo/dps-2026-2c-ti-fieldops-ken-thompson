package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class OccupyingExpeditions {
    private final List<Expedition> plans;

    private OccupyingExpeditions(List<Expedition> plans) {
        this.plans = plans;
    }

    public static OccupyingExpeditions none() {
        return new OccupyingExpeditions(List.of());
    }

    public static OccupyingExpeditions of(
            Expedition plan,
            List<Expedition> others,
            Map<ExpeditionId, ExpeditionExecution> executions
    ) {
        Objects.requireNonNull(plan, "expedition");
        Objects.requireNonNull(others, "other expeditions");
        Objects.requireNonNull(executions, "executions");
        Set<ExpeditionId> lineage = Revisions.lineage(plan, others);
        List<Expedition> occupying = others.stream()
                .filter(other -> !lineage.contains(other.id()))
                .filter(other -> occupies(other, executions))
                .toList();
        return new OccupyingExpeditions(occupying);
    }

    public List<Expedition> plans() {
        return plans;
    }

    public List<TemporalBooking> bookings() {
        List<TemporalBooking> bookings = new ArrayList<>();
        for (Expedition plan : plans) {
            bookings.addAll(TemporalBooking.of(plan));
        }
        return bookings;
    }

    private static boolean occupies(Expedition other, Map<ExpeditionId, ExpeditionExecution> executions) {
        Optional<ExpeditionExecution> run = Optional.ofNullable(executions.get(other.id()));
        if (run.filter(ExpeditionExecution::isFinished).isPresent()) {
            return false;
        }
        return other.status().occupiesResources()
                || other.status() == ExpeditionStatus.SUPERSEDED && run.isPresent();
    }
}
