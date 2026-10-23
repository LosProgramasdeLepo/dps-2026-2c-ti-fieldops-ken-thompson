package edu.itba.fieldops.domain.support;

import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.PlanningContexts;

import java.util.List;

public final class Submissions {
    private Submissions() {
    }

    public static void submit(Expedition plan) {
        plan.submitForReview(
                context -> new ValidationResult(context.plan().id(), context.plan().version(), List.of()),
                new PlanningContexts(new StoredExpeditions(), new StoredRuns(), new ResourceCatalog().catalogs())
        );
    }
}
