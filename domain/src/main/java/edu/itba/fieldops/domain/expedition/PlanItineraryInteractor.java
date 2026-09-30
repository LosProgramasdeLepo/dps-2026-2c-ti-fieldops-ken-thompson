package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.Objects;

public final class PlanItineraryInteractor implements PlanItinerary {
    private final ExpeditionRepository plans;

    public PlanItineraryInteractor(ExpeditionRepository plans) {
        this.plans = Objects.requireNonNull(plans, "plans");
    }

    @Override
    public void addActivity(ExpeditionId expeditionId, Activity activity) {
        Expedition expedition = require(expeditionId);
        expedition.addActivity(activity);
        plans.save(expedition);
    }

    @Override
    public void addDependency(ExpeditionId expeditionId, ActivityId activityId, ActivityId predecessorId) {
        Expedition expedition = require(expeditionId);
        expedition.addDependency(activityId, predecessorId);
        plans.save(expedition);
    }

    private Expedition require(ExpeditionId expeditionId) {
        return plans.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidValue("unknown expedition: " + expeditionId));
    }
}
