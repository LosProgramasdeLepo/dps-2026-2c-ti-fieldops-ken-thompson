package edu.itba.fieldops.domain.support;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.Plans;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StoredExpeditions implements Plans {
    private final Map<ExpeditionId, Expedition> plans = new LinkedHashMap<>();

    public StoredExpeditions(Expedition... plans) {
        for (Expedition plan : plans) {
            this.plans.put(plan.id(), plan);
        }
    }

    @Override
    public Optional<Expedition> find(ExpeditionId id) {
        return Optional.ofNullable(plans.get(id));
    }

    @Override
    public List<Expedition> all() {
        return List.copyOf(plans.values());
    }
}
