package edu.itba.fieldops.domain.expedition.usecase;

import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PlanSnapshot(
        ExpeditionId id,
        int version,
        Optional<ExpeditionId> supersedes,
        ExpeditionStatus status,
        ExpeditionCharter charter,
        List<ItineraryItem> itinerary,
        List<Assignment> assignments,
        List<PermitId> permits,
        List<AcceptedWarning> acceptedWarnings
) {
    public PlanSnapshot {
        Objects.requireNonNull(id, "expedition id");
        Objects.requireNonNull(supersedes, "superseded plan");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(charter, "charter");
        itinerary = List.copyOf(itinerary);
        assignments = List.copyOf(assignments);
        permits = List.copyOf(permits);
        acceptedWarnings = List.copyOf(acceptedWarnings);
    }

    public static PlanSnapshot of(Expedition plan) {
        return new PlanSnapshot(
                plan.id(),
                plan.version(),
                plan.supersedes(),
                plan.status(),
                plan.charter(),
                plan.items(),
                plan.assignments().all(),
                plan.permits(),
                plan.acceptedWarnings()
        );
    }
}
