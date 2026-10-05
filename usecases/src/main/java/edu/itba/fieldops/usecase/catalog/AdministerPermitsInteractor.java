package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.Objects;

public final class AdministerPermitsInteractor implements AdministerPermits {
    private final PermitRegistry permits;

    public AdministerPermitsInteractor(PermitRegistry permits) {
        this.permits = Objects.requireNonNull(permits, "permits");
    }

    @Override
    public PermitId registerPermit(PermitKind kind, WorkZone zone, TimePeriod validity) {
        PermitId id = permits.nextPermitId();
        permits.save(new Permit(id, zone, validity, kind));
        return id;
    }
}
