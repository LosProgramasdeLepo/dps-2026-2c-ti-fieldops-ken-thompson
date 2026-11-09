package edu.itba.fieldops.domain.usecase.audit;

import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;

import java.util.List;
import java.util.Set;

public interface ConsultEvents {
    Page<RecordedEvent> ofExpedition(ExpeditionId expeditionId, Set<String> types, PageRequest request);

    Page<RecordedEvent> all(Set<String> types, PageRequest request);

    RecordedEvent event(EventId eventId);

    List<Delivery> deliveries(EventId eventId);
}
