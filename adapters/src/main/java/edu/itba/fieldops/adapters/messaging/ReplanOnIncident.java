package edu.itba.fieldops.adapters.messaging;

import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.usecase.expedition.RespondToIncident;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ReplanOnIncident implements EventConsumer {
    public static final String NAME = "replan-on-incident";

    private final RespondToIncident incidents;

    public ReplanOnIncident(RespondToIncident incidents) {
        this.incidents = Objects.requireNonNull(incidents, "incidents");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean subscribesTo(DomainEvent event) {
        return event instanceof IncidentReported reported && reported.activityId().isPresent();
    }

    @Override
    public void consume(RecordedEvent event) {
        IncidentReported reported = (IncidentReported) event.event();
        incidents.respondTo(reported.expeditionId(), reported.incidentId());
    }
}
