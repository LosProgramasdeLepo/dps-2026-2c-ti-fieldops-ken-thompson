package edu.itba.fieldops.api.event;

import edu.itba.fieldops.api.event.EventResponses.EventResponse;
import edu.itba.fieldops.api.json.PageQuery;
import edu.itba.fieldops.api.json.PageResponse;
import edu.itba.fieldops.api.json.Pages;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.usecase.audit.ConsultEvents;
import edu.itba.fieldops.domain.usecase.audit.ManageEventDeliveries;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public final class EventsController {
    private final ConsultEvents events;
    private final ManageEventDeliveries deliveries;

    public EventsController(ConsultEvents events, ManageEventDeliveries deliveries) {
        this.events = events;
        this.deliveries = deliveries;
    }

    @GetMapping("/v1/expeditions/{id}/events")
    public ResponseEntity<PageResponse<EventResponse>> ofExpedition(
            @PathVariable UUID id,
            @Valid PageQuery page,
            @Valid EventQuery query
    ) {
        return Pages.toResponse(
                events.ofExpedition(new ExpeditionId(id), query.types(), Pages.toRequest(page)),
                this::response
        );
    }

    @GetMapping("/v1/events")
    public ResponseEntity<PageResponse<EventResponse>> all(@Valid PageQuery page, @Valid EventQuery query) {
        return Pages.toResponse(events.all(query.types(), Pages.toRequest(page)), this::response);
    }

    @GetMapping("/v1/events/{eventId}")
    public EventResponse event(@PathVariable UUID eventId) {
        return response(events.event(new EventId(eventId)));
    }

    @PostMapping("/v1/events/{eventId}/deliveries/{consumer}/retry")
    public ResponseEntity<Void> retry(@PathVariable UUID eventId, @PathVariable String consumer) {
        deliveries.retry(new EventId(eventId), consumer);
        return Responses.noContent();
    }

    private EventResponse response(RecordedEvent recorded) {
        return EventMapping.event(recorded, events.deliveries(recorded.id()));
    }
}
