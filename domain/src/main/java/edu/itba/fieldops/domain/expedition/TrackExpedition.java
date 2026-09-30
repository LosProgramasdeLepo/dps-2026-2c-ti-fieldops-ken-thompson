package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Observation;

public interface TrackExpedition {
    void start(ExpeditionId expeditionId);

    void suspend(ExpeditionId expeditionId);

    void resume(ExpeditionId expeditionId);

    void finish(ExpeditionId expeditionId);

    void startActivity(ExpeditionId expeditionId, ActivityId activityId);

    void finishActivity(ExpeditionId expeditionId, ActivityId activityId, String result);

    void addIncident(ExpeditionId expeditionId, Incident incident);

    void addObservation(ExpeditionId expeditionId, Observation observation);

    void returnToDraft(ExpeditionId expeditionId);
}
