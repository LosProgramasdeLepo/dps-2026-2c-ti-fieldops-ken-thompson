package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.EventId;

import java.util.List;

public interface Deliveries {
    List<Delivery> of(EventId id);
}
