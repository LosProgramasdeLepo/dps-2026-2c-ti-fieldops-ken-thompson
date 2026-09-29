package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.DomainException;

public final class InvalidAssignment extends DomainException {
    public InvalidAssignment(String message) {
        super(message);
    }
}
