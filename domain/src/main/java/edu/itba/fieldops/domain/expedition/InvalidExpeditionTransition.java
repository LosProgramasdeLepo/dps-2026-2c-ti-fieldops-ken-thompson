package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.DomainException;

public final class InvalidExpeditionTransition extends DomainException {
    public InvalidExpeditionTransition(Enum<?> current, String action) {
        super("cannot " + action + " while expedition is " + current);
    }
}
