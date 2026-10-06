package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.Objects;

public final class ConsultExpeditionInteractor implements ConsultExpedition {
    private final ExpeditionRepository plans;

    public ConsultExpeditionInteractor(ExpeditionRepository plans) {
        this.plans = Objects.requireNonNull(plans, "plans");
    }

    @Override
    public PlanSnapshot of(ExpeditionId expeditionId) {
        return PlanSnapshot.of(plans.require(expeditionId));
    }

    @Override
    public Page<PlanSnapshot> all(PageRequest request) {
        return plans.all(request).map(PlanSnapshot::of);
    }

    @Override
    public Activity activity(ExpeditionId expeditionId, ActivityId activityId) {
        Objects.requireNonNull(activityId, "activity id");
        return plans.require(expeditionId).activities().stream()
                .filter(activity -> activity.id().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new UnknownResource("activity", activityId.value().toString()));
    }
}
