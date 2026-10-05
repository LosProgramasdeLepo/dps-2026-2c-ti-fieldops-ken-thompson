package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.DomainException;

public final class ExpeditionNotApprovable extends DomainException {
    public ExpeditionNotApprovable(String reason) {
        super(reason);
    }
}
