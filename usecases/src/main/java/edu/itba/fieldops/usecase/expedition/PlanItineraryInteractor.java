package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;

import java.util.Objects;

public final class PlanItineraryInteractor implements PlanItinerary {
    private final ExpeditionRepository plans;

    public PlanItineraryInteractor(ExpeditionRepository plans) {
        this.plans = Objects.requireNonNull(plans, "plans");
    }

    @Override
    public ActivityId nextActivityId() {
        return plans.nextActivityId();
    }

    @Override
    public void addActivity(ExpeditionId expeditionId, Activity activity) {
        Expedition expedition = plans.require(expeditionId);
        expedition.addActivity(activity);
        plans.save(expedition);
    }

    @Override
    public void addBlock(ExpeditionId expeditionId, ActivityBlock block) {
        Expedition expedition = plans.require(expeditionId);
        expedition.addBlock(block);
        plans.save(expedition);
    }

    @Override
    public void addDependency(ExpeditionId expeditionId, ActivityId activityId, ActivityId predecessorId) {
        Expedition expedition = plans.require(expeditionId);
        expedition.addDependency(activityId, predecessorId);
        plans.save(expedition);
    }
}
