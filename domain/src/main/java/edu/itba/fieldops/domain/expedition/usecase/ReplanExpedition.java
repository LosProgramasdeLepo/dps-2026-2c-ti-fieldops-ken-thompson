package edu.itba.fieldops.domain.expedition.usecase;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.time.Duration;

public interface ReplanExpedition {
    ExpeditionId revise(ExpeditionId approvedId);

    void cancel(ExpeditionId draftId, ActivityId activityId);

    void delay(ExpeditionId draftId, ActivityId activityId, Duration delay);

    void replaceUnavailable(ExpeditionId draftId);
}
