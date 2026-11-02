package edu.itba.fieldops.domain.usecase.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.IncidentDescription;

public interface RecordIncident {
    void record(ExpeditionId expeditionId, IncidentDescription description);

    void record(ExpeditionId expeditionId, IncidentDescription description, ActivityId activityId);
}
