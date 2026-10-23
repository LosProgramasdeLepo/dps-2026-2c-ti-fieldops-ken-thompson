package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;

public interface ExpeditionRepository extends Plans {
    ExpeditionId nextId();

    ActivityId nextActivityId();

    void save(Expedition expedition);

    Page<Expedition> all(PageRequest request);
}
