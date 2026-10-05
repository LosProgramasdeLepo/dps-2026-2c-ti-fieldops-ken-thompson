package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.PermitId;

import java.util.Optional;

public interface Permits {
    Optional<Permit> permit(PermitId id);
}
