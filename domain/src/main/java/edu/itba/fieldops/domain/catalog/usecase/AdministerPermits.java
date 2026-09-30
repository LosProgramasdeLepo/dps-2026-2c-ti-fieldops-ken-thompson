package edu.itba.fieldops.domain.catalog.usecase;

import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

public interface AdministerPermits {
    PermitId registerPermit(PermitKind kind, WorkZone zone, TimePeriod validity);
}
