package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;

import java.util.Objects;
import java.util.Optional;

public interface Events {
    Page<RecordedEvent> page(EventFilter filter, PageRequest request);

    Optional<RecordedEvent> find(EventId id);

    default RecordedEvent require(EventId id) {
        return find(Objects.requireNonNull(id, "event id"))
                .orElseThrow(() -> new UnknownResource("event", id.value().toString()));
    }
}
