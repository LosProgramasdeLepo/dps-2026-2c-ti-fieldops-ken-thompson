package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryExpeditionRepository implements ExpeditionRepository {
    private final Map<ExpeditionId, Expedition> plans = new LinkedHashMap<>();

    @Override
    public ExpeditionId nextId() {
        return new ExpeditionId(UUID.randomUUID());
    }

    @Override
    public void save(Expedition expedition) {
        Objects.requireNonNull(expedition, "expedition");
        plans.put(expedition.id(), expedition);
    }

    @Override
    public Optional<Expedition> find(ExpeditionId id) {
        Objects.requireNonNull(id, "expedition id");
        return Optional.ofNullable(plans.get(id));
    }

    @Override
    public List<Expedition> all() {
        return List.copyOf(plans.values());
    }
}
