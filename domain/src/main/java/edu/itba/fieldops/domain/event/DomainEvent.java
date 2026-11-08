package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.time.Instant;

public sealed interface DomainEvent permits
        ExpeditionApproved,
        RunStarted,
        ActivityStarted,
        ActivityCompleted,
        IncidentReported,
        RunFinished,
        ReplanProposed,
        ReplanDecided {
    ExpeditionId expeditionId();

    Instant occurredAt();

    default String type() {
        return getClass().getSimpleName();
    }
}
