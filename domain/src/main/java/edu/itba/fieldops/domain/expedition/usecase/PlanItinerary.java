package edu.itba.fieldops.domain.expedition.usecase;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Activity;

public interface PlanItinerary {
    void addActivity(ExpeditionId expeditionId, Activity activity);

    void addDependency(ExpeditionId expeditionId, ActivityId activityId, ActivityId predecessorId);
}
