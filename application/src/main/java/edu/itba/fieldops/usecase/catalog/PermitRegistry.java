package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Permits;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.Objects;

public interface PermitRegistry extends Permits {
    PermitId nextPermitId();

    void save(Permit permit);

    Page<Permit> permits(PageRequest request);

    default Permit require(PermitId id) {
        return permit(Objects.requireNonNull(id, "permit id"))
                .orElseThrow(() -> new UnknownResource("permit", id.value().toString()));
    }
}
