package edu.itba.fieldops.domain.shared;

public final class InvalidExpeditionTransition extends DomainException {
    public InvalidExpeditionTransition(Enum<?> current, String action) {
        super("cannot " + action + " while expedition is " + current);
    }
}
