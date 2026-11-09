package edu.itba.fieldops.api.event;

import java.util.List;
import java.util.Set;

public record EventQuery(List<@KnownEventType String> type) {
    public EventQuery {
        type = type == null ? List.of() : List.copyOf(type);
    }

    Set<String> types() {
        return Set.copyOf(type);
    }
}
