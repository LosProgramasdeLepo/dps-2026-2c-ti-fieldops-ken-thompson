package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.InMemoryCertificationRegistry;
import edu.itba.fieldops.adapters.InMemoryConsumableRegistry;
import edu.itba.fieldops.adapters.InMemoryExecutionRepository;
import edu.itba.fieldops.adapters.InMemoryExpeditionRepository;
import edu.itba.fieldops.adapters.InMemoryInstrumentRegistry;
import edu.itba.fieldops.adapters.InMemoryPermitRegistry;
import edu.itba.fieldops.adapters.InMemoryPersonRegistry;
import edu.itba.fieldops.adapters.InMemoryReplanProposalRepository;
import edu.itba.fieldops.adapters.InMemoryVehicleRegistry;
import edu.itba.fieldops.domain.shared.Clock;

public final class InMemoryFieldOps {
    private final FieldOpsRepositories repositories = new FieldOpsRepositories(
            new InMemoryCertificationRegistry(),
            new InMemoryPersonRegistry(),
            new InMemoryVehicleRegistry(),
            new InMemoryInstrumentRegistry(),
            new InMemoryConsumableRegistry(),
            new InMemoryPermitRegistry(),
            new InMemoryExpeditionRepository(),
            new InMemoryExecutionRepository(),
            new InMemoryReplanProposalRepository()
    );
    private final FieldOps useCases;

    public InMemoryFieldOps(Clock clock) {
        this.useCases = new FieldOps(repositories, clock);
    }

    public FieldOpsRepositories repositories() {
        return repositories;
    }

    public FieldOps useCases() {
        return useCases;
    }
}
