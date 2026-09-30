package edu.itba.fieldops.domain.expedition.usecase;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;

public interface TrackExpedition {
    void start(ExpeditionId expeditionId);

    void suspend(ExpeditionId expeditionId);

    void resume(ExpeditionId expeditionId);

    void finish(ExpeditionId expeditionId);

    void startActivity(ExpeditionId expeditionId, ActivityId activityId);

    void finishActivity(ExpeditionId expeditionId, ActivityId activityId, String result);

    void addObservation(ExpeditionId expeditionId, String text);
}
