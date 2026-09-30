package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.details.InMemoryExecutionRepository;
import edu.itba.fieldops.details.InMemoryExpeditionRepository;
import edu.itba.fieldops.details.ResourceCatalog;
import edu.itba.fieldops.domain.validation.RuleBasedValidator;

public final class Approvals {
    private Approvals() {
    }

    public static void approve(Expedition expedition, ResourceCatalog catalog) {
        InMemoryExpeditionRepository plans = new InMemoryExpeditionRepository();
        plans.save(expedition);
        new ApproveExpeditionInteractor(
                plans,
                new InMemoryExecutionRepository(),
                catalog.catalogs(),
                RuleBasedValidator.withDefaultRules()
        ).approve(expedition.id());
    }
}
