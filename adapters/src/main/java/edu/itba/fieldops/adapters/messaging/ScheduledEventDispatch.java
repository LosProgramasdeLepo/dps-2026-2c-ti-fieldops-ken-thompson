package edu.itba.fieldops.adapters.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBooleanProperty(name = "fieldops.events.dispatcher.enabled", matchIfMissing = true)
public class ScheduledEventDispatch {
    private final EventDispatcher dispatcher;

    public ScheduledEventDispatch(EventDispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    @Scheduled(fixedDelayString = "${fieldops.events.dispatch-delay:500ms}")
    public void dispatch() {
        dispatcher.dispatchDue();
    }
}
