package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OccupyingExpeditions {
    private final List<Expedition> plans;

    private OccupyingExpeditions(List<Expedition> plans) {
        this.plans = plans;
    }

    public static OccupyingExpeditions none() {
        return new OccupyingExpeditions(List.of());
    }

    public static OccupyingExpeditions of(Expedition plan, List<Expedition> others) {
        return of(plan, others, Map.of());
    }

    public static OccupyingExpeditions of(
            Expedition plan,
            List<Expedition> others,
            Map<ExpeditionId, ExpeditionExecution> executions
    ) {
        Objects.requireNonNull(plan, "expedition");
        Objects.requireNonNull(others, "other expeditions");
        Objects.requireNonNull(executions, "executions");
        List<Expedition> occupying = new ArrayList<>();
        for (Expedition peer : others) {
            if (peer.id().equals(plan.id())) {
                continue;
            }
            if (plan.supersedes().filter(peer.id()::equals).isPresent()) {
                continue;
            }
            ExpeditionExecution execution = executions.get(peer.id());
            if (execution != null && execution.isFinished()) {
                continue;
            }
            if (peer.status().occupiesResources() || occupiesWhileRunning(peer, execution)) {
                occupying.add(peer);
            }
        }
        return new OccupyingExpeditions(List.copyOf(occupying));
    }

    public List<Expedition> plans() {
        return plans;
    }

    private static boolean occupiesWhileRunning(Expedition peer, ExpeditionExecution execution) {
        return peer.status() == ExpeditionStatus.SUPERSEDED && execution != null;
    }
}
