package edu.itba.fieldops.domain.expedition;

import java.time.Instant;
import java.util.Objects;

record Approval(ExpeditionValidator validator, PlanningContexts contexts, Instant at) {
    Approval {
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(contexts, "planning contexts");
        Objects.requireNonNull(at, "approval time");
    }
}
