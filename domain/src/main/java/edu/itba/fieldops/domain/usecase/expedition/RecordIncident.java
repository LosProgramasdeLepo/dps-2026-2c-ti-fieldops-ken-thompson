package edu.itba.fieldops.domain.usecase.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.tracking.IncidentDescription;

public interface RecordIncident {
    IncidentId record(ExpeditionId expeditionId, IncidentDescription description);

    IncidentId record(ExpeditionId expeditionId, IncidentDescription description, ActivityId activityId);

    IncidentSnapshot incident(ExpeditionId runId, IncidentId incidentId);
}
