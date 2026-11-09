package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.shared.DomainException;

public final class InvalidDeliveryTransition extends DomainException {
    public InvalidDeliveryTransition(String consumer, DeliveryStatus status) {
        super("delivery to " + consumer + " cannot be retried from " + status);
    }
}
