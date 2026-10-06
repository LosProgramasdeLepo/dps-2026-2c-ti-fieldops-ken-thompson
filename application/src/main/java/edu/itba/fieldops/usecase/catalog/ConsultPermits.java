package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

public interface ConsultPermits {
    Page<Permit> permits(PageRequest request);

    Permit permit(PermitId permitId);
}
