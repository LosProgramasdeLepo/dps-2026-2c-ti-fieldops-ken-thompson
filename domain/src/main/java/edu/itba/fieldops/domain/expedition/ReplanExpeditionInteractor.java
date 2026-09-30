package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.expedition.usecase.ReplanExpedition;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.ExecutionRepository;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

public final class ReplanExpeditionInteractor implements ReplanExpedition {
    private final ExpeditionRepository plans;
    private final PlanningContexts contexts;
    private final Replanner replanner;

    public ReplanExpeditionInteractor(
            ExpeditionRepository plans,
            ExecutionRepository executions,
            Catalogs catalogs,
            Replanner replanner
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.contexts = new PlanningContexts(plans, executions, catalogs);
        this.replanner = Objects.requireNonNull(replanner, "replanner");
    }

    @Override
    public ExpeditionId cancel(ExpeditionId expeditionId, ActivityId activityId) {
        return replan(expeditionId, context -> replanner.cancel(context, activityId));
    }

    @Override
    public ExpeditionId delay(ExpeditionId expeditionId, ActivityId activityId, Duration delay) {
        return replan(expeditionId, context -> replanner.delay(context, activityId, delay));
    }

    @Override
    public ExpeditionId replaceUnavailable(ExpeditionId expeditionId) {
        return replan(expeditionId, replanner::replaceUnavailable);
    }

    private ExpeditionId replan(ExpeditionId expeditionId, Consumer<PlanningContext> change) {
        Expedition original = plans.require(expeditionId);
        returnToDraftIfInReview(original);
        Expedition working = workingDraftOf(original);
        change.accept(contexts.around(working));
        return persist(original, working);
    }

    private static void returnToDraftIfInReview(Expedition plan) {
        if (plan.status() == ExpeditionStatus.IN_REVIEW) {
            plan.returnToDraft();
        }
    }

    private Expedition workingDraftOf(Expedition original) {
        if (original.status() == ExpeditionStatus.APPROVED) {
            return original.reviseAsDraft(plans.nextId());
        }
        return original;
    }

    private ExpeditionId persist(Expedition original, Expedition working) {
        if (!working.id().equals(original.id())) {
            original.markSuperseded();
            plans.save(original);
        }
        plans.save(working);
        return working.id();
    }
}
