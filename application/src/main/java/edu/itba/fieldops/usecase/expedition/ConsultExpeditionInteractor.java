package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.usecase.expedition.ConsultExpedition;
import edu.itba.fieldops.domain.usecase.expedition.PlanSnapshot;

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
        Expedition plan = plans.require(expeditionId);
        if (!plan.includes(activityId)) {
            throw new UnknownResource("activity", activityId.value().toString());
        }
        return plan.activityOf(activityId);
    }
}
