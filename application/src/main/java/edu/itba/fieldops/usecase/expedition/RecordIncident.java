package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;

public interface RecordIncident {
    void record(ExpeditionId expeditionId, String description);

    void record(ExpeditionId expeditionId, String description, ActivityId activityId);
}
