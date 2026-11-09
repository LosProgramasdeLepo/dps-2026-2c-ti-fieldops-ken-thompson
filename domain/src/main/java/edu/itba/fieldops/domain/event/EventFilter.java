package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.Objects;
import java.util.Set;

public record EventFilter(Set<ExpeditionId> expeditions, Set<String> types) {
    public EventFilter {
        expeditions = Set.copyOf(Objects.requireNonNull(expeditions, "expeditions"));
        types = Set.copyOf(Objects.requireNonNull(types, "types"));
        types.stream()
                .filter(type -> !EventTypes.known().contains(type))
                .findFirst()
                .ifPresent(type -> {
                    throw new InvalidValue("unknown event type: " + type);
                });
    }

    public static EventFilter all() {
        return new EventFilter(Set.of(), Set.of());
    }

    public static EventFilter forExpeditions(Set<ExpeditionId> expeditions) {
        return new EventFilter(expeditions, Set.of());
    }

    public EventFilter withTypes(Set<String> types) {
        return new EventFilter(expeditions, types);
    }
}
