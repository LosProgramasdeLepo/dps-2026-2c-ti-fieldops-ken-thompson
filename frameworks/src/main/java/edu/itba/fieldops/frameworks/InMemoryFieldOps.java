package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.InMemoryExecutionRepository;
import edu.itba.fieldops.adapters.InMemoryExpeditionRepository;
import edu.itba.fieldops.adapters.InMemoryReplanProposalRepository;
import edu.itba.fieldops.adapters.ResourceCatalog;
import edu.itba.fieldops.usecase.shared.Clock;

public final class InMemoryFieldOps {
    private final ResourceCatalog catalog = new ResourceCatalog();
    private final InMemoryExpeditionRepository plans = new InMemoryExpeditionRepository();
    private final InMemoryExecutionRepository runs = new InMemoryExecutionRepository();
    private final InMemoryReplanProposalRepository proposals = new InMemoryReplanProposalRepository();
    private final FieldOps useCases;

    public InMemoryFieldOps(Clock clock) {
        this.useCases = new FieldOps(repositories(), clock);
    }

    public FieldOpsRepositories repositories() {
        return new FieldOpsRepositories(catalog, catalog, catalog, catalog, catalog, catalog, plans, runs, proposals);
    }

    public FieldOps useCases() {
        return useCases;
    }

    public ResourceCatalog catalog() {
        return catalog;
    }

    public InMemoryExpeditionRepository plans() {
        return plans;
    }

    public InMemoryExecutionRepository runs() {
        return runs;
    }

    public InMemoryReplanProposalRepository proposals() {
        return proposals;
    }
}
