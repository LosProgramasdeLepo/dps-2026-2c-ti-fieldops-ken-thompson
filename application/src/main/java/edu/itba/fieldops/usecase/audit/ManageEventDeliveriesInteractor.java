package edu.itba.fieldops.usecase.audit;

import edu.itba.fieldops.domain.event.EventDeliveries;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.usecase.audit.ManageEventDeliveries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class ManageEventDeliveriesInteractor implements ManageEventDeliveries {
    private final EventDeliveries deliveries;
    private final Clock clock;

    public ManageEventDeliveriesInteractor(EventDeliveries deliveries, Clock clock) {
        this.deliveries = Objects.requireNonNull(deliveries, "deliveries");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void retry(EventId eventId, String consumer) {
        deliveries.of(eventId).stream()
                .filter(delivery -> delivery.consumer().equals(consumer))
                .findFirst()
                .orElseThrow(() -> new UnknownResource("delivery", consumer))
                .requireRetriable();
        deliveries.retry(eventId, consumer, clock.now());
    }
}
