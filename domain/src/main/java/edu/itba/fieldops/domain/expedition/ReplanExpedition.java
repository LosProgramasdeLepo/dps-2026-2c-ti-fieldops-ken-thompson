package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.time.Duration;

public interface ReplanExpedition {
    ExpeditionId cancel(ExpeditionId expeditionId, ActivityId activityId);

    ExpeditionId delay(ExpeditionId expeditionId, ActivityId activityId, Duration delay);

    ExpeditionId replaceUnavailable(ExpeditionId expeditionId);
}
