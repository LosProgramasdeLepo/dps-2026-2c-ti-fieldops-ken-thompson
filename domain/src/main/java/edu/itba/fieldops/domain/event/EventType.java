package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.Objects;

public record EventType(String value) {
    public EventType {
        Objects.requireNonNull(value, "event type");
        if (!EventTypes.known().contains(value)) {
            throw new InvalidValue("unknown event type: " + value);
        }
    }

    public static EventType of(DomainEvent event) {
        return new EventType(Objects.requireNonNull(event, "event").type());
    }
}
