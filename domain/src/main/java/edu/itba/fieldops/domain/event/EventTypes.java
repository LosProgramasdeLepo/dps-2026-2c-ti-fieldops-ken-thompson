package edu.itba.fieldops.domain.event;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class EventTypes {
    private static final Set<String> KNOWN = Arrays.stream(DomainEvent.class.getPermittedSubclasses())
            .map(Class::getSimpleName)
            .collect(Collectors.toUnmodifiableSet());

    private EventTypes() {
    }

    public static Set<String> known() {
        return KNOWN;
    }
}
