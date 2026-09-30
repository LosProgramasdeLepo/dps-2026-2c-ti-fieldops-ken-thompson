package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.expedition.usecase.ReplanExpedition;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;

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
    public ExpeditionId revise(ExpeditionId approvedId) {
        Expedition revision = plans.require(approvedId).reviseAsDraft(plans.nextId());
        plans.save(revision);
        return revision.id();
    }

    @Override
    public void cancel(ExpeditionId draftId, ActivityId activityId) {
        replan(draftId, context -> replanner.cancel(context, activityId));
    }

    @Override
    public void delay(ExpeditionId draftId, ActivityId activityId, Duration delay) {
        replan(draftId, context -> replanner.delay(context, activityId, delay));
    }

    @Override
    public void replaceUnavailable(ExpeditionId draftId) {
        replan(draftId, replanner::replaceUnavailable);
    }

    private void replan(ExpeditionId draftId, Consumer<PlanningContext> change) {
        Expedition draft = plans.require(draftId);
        change.accept(contexts.around(draft));
        plans.save(draft);
    }
}
