package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.shared.DomainException;

public final class InvalidActivityExecution extends DomainException {
    public InvalidActivityExecution(String message) {
        super(message);
    }
}
