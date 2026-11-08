package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.Clock;

import java.util.Objects;
import java.util.Optional;

public final class ExpeditionApprover {
    private final ExpeditionValidator validator;
    private final PlanningContexts contexts;
    private final Clock clock;

    public ExpeditionApprover(ExpeditionValidator validator, PlanningContexts contexts, Clock clock) {
        this.validator = Objects.requireNonNull(validator, "validator");
        this.contexts = Objects.requireNonNull(contexts, "planning contexts");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void approve(Expedition plan, Optional<Expedition> predecessor) {
        Objects.requireNonNull(plan, "plan").approve(new Approval(validator, contexts, clock.now()), predecessor);
    }
}
