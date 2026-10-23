package edu.itba.fieldops.domain.usecase.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;

public interface ConsultExpedition {
    PlanSnapshot of(ExpeditionId expeditionId);

    Page<PlanSnapshot> all(PageRequest request);

    Activity activity(ExpeditionId expeditionId, ActivityId activityId);
}
