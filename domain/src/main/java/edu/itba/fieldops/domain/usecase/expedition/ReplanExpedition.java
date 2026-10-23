package edu.itba.fieldops.domain.usecase.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Delay;

public interface ReplanExpedition {
    ExpeditionId revise(ExpeditionId approvedId);

    void cancel(ExpeditionId draftId, ActivityId activityId);

    void delay(ExpeditionId draftId, ActivityId activityId, Delay delay);

    void replaceUnavailable(ExpeditionId draftId);
}
