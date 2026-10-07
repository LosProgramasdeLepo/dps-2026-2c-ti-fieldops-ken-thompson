package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.Objects;

public final class ConsultPermitsInteractor implements ConsultPermits {
    private final PermitRegistry permits;

    public ConsultPermitsInteractor(PermitRegistry permits) {
        this.permits = Objects.requireNonNull(permits, "permits");
    }

    @Override
    public Page<Permit> permits(PageRequest request) {
        return permits.permits(request);
    }

    @Override
    public Permit permit(PermitId permitId) {
        return permits.require(permitId);
    }
}
