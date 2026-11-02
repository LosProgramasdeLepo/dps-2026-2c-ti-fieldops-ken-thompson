package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.Replanner;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Delay;
import edu.itba.fieldops.domain.usecase.expedition.ReplanExpedition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.function.Consumer;

@Service
@Transactional
public class ReplanExpeditionInteractor implements ReplanExpedition {
    private final ExpeditionRepository plans;
    private final Replanner replanner;

    public ReplanExpeditionInteractor(ExpeditionRepository plans, Replanner replanner) {
        this.plans = Objects.requireNonNull(plans, "plans");
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
        replan(draftId, draft -> replanner.cancel(draft, activityId));
    }

    @Override
    public void delay(ExpeditionId draftId, ActivityId activityId, Delay delay) {
        replan(draftId, draft -> replanner.delay(draft, activityId, delay));
    }

    @Override
    public void replaceUnavailable(ExpeditionId draftId) {
        replan(draftId, replanner::replaceUnavailable);
    }

    private void replan(ExpeditionId draftId, Consumer<Expedition> change) {
        Expedition draft = plans.require(draftId);
        change.accept(draft);
        plans.save(draft);
    }
}
