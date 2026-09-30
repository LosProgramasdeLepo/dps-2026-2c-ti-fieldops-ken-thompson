package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.expedition.usecase.ReplanExpedition;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.ExecutionRepository;

import java.time.Duration;
import java.util.Objects;

public final class ReplanExpeditionInteractor implements ReplanExpedition {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final BookableResources resources;
    private final Replanner replanner;

    public ReplanExpeditionInteractor(
            ExpeditionRepository plans,
            ExecutionRepository executions,
            BookableResources resources,
            Replanner replanner
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.executions = Objects.requireNonNull(executions, "executions");
        this.resources = Objects.requireNonNull(resources, "bookable resources");
        this.replanner = Objects.requireNonNull(replanner, "replanner");
    }

    @Override
    public ExpeditionId cancel(ExpeditionId expeditionId, ActivityId activityId) {
        Expedition original = require(expeditionId);
        Expedition working = draftOf(original);
        replanner.cancel(working, activityId, resources, Peers.around(working, plans, executions));
        return persist(original, working);
    }

    @Override
    public ExpeditionId delay(ExpeditionId expeditionId, ActivityId activityId, Duration delay) {
        Expedition original = require(expeditionId);
        Expedition working = draftOf(original);
        replanner.delay(working, activityId, delay, resources, Peers.around(working, plans, executions));
        return persist(original, working);
    }

    @Override
    public ExpeditionId replaceUnavailable(ExpeditionId expeditionId) {
        Expedition original = require(expeditionId);
        Expedition working = draftOf(original);
        replanner.replaceUnavailable(working, resources, Peers.around(working, plans, executions));
        return persist(original, working);
    }

    private static Expedition draftOf(Expedition original) {
        return original.status() == ExpeditionStatus.APPROVED ? original.reviseAsDraft() : original;
    }

    private ExpeditionId persist(Expedition original, Expedition working) {
        if (!working.id().equals(original.id())) {
            original.markSuperseded();
            plans.save(original);
        }
        plans.save(working);
        return working.id();
    }

    private Expedition require(ExpeditionId expeditionId) {
        return plans.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidValue("unknown expedition: " + expeditionId));
    }
}
