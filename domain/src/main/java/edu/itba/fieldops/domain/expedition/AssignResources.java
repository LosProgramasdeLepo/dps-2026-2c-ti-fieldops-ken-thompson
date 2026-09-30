package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;

import java.util.List;

public interface AssignResources {
    void addAssignment(ExpeditionId expeditionId, Assignment assignment);

    void addPermit(ExpeditionId expeditionId, PermitId permitId);

    List<Assignment> suggest(ExpeditionId expeditionId);
}
