package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.PermitId;

public interface PermitRegistry extends Permits {
    PermitId nextPermitId();

    void save(Permit permit);
}
