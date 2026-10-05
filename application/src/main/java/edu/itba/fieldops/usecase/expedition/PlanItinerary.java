package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;

public interface PlanItinerary {
    ActivityId nextActivityId();

    void addActivity(ExpeditionId expeditionId, Activity activity);

    void addBlock(ExpeditionId expeditionId, ActivityBlock block);

    void addDependency(ExpeditionId expeditionId, ActivityId activityId, ActivityId predecessorId);
}
