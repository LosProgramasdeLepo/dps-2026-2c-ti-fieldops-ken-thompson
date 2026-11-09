package edu.itba.fieldops.usecase.audit;

import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.Events;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.Plans;
import edu.itba.fieldops.domain.expedition.Revisions;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.usecase.audit.ConsultEvents;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ConsultEventsInteractor implements ConsultEvents {
    private final Plans plans;
    private final Events events;

    public ConsultEventsInteractor(Plans plans, Events events) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.events = Objects.requireNonNull(events, "events");
    }

    @Override
    public Page<RecordedEvent> ofExpedition(ExpeditionId expeditionId, Set<String> types, PageRequest request) {
        Expedition plan = plans.require(expeditionId);
        Set<ExpeditionId> family = Revisions.family(plan, plans.all());
        return events.page(EventFilter.forExpeditions(family).withTypes(types), request);
    }

    @Override
    public Page<RecordedEvent> all(Set<String> types, PageRequest request) {
        return events.page(EventFilter.all().withTypes(types), request);
    }

    @Override
    public RecordedEvent event(EventId eventId) {
        return events.require(eventId);
    }
}
