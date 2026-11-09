package edu.itba.fieldops.adapters.messaging;

import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.usecase.notification.NotifyResponsibles;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class NotifyResponsiblesConsumer implements EventConsumer {
    public static final String NAME = "notify-responsibles";

    private final NotifyResponsibles responsibles;

    public NotifyResponsiblesConsumer(NotifyResponsibles responsibles) {
        this.responsibles = Objects.requireNonNull(responsibles, "notify responsibles");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean subscribesTo(DomainEvent event) {
        return responsibles.concerns(event);
    }

    @Override
    public void consume(RecordedEvent event) {
        responsibles.about(event);
    }
}
