package edu.itba.fieldops.domain.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;

public interface ConsultPermits {
    Page<Permit> permits(PageRequest request);

    Permit permit(PermitId permitId);
}
