package edu.itba.fieldops.domain.usecase.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.Outcome;

public interface TrackExpedition {
    void start(ExpeditionId expeditionId);

    void suspend(ExpeditionId expeditionId);

    void resume(ExpeditionId expeditionId);

    void finish(ExpeditionId expeditionId);

    void startActivity(ExpeditionId expeditionId, ActivityId activityId);

    void finishActivity(ExpeditionId expeditionId, ActivityId activityId, Outcome result);

    void addObservation(ExpeditionId expeditionId, String text);

    RunSnapshot run(ExpeditionId expeditionId);
}
