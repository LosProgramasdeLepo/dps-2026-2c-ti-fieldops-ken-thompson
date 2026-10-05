package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Permits;
import edu.itba.fieldops.domain.identity.PermitId;

public interface PermitRegistry extends Permits {
    PermitId nextPermitId();

    void save(Permit permit);
}
