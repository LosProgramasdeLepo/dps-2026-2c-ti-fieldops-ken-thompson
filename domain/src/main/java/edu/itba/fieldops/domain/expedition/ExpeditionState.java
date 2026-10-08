package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ExpeditionState(
        ExpeditionId id,
        int version,
        Optional<ExpeditionId> supersedes,
        ExpeditionStatus status,
        ExpeditionCharter charter,
        List<ItineraryItem> items,
        List<Assignment> assignments,
        List<PermitId> permits,
        List<AcceptedWarning> acceptedWarnings
) {
    public ExpeditionState {
        Objects.requireNonNull(id, "expedition id");
        Objects.requireNonNull(supersedes, "supersedes");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(charter, "charter");
        items = List.copyOf(items);
        assignments = List.copyOf(assignments);
        permits = List.copyOf(permits);
        acceptedWarnings = List.copyOf(acceptedWarnings);
        if (version < 1) {
            throw new InvalidValue("version must be positive: " + version);
        }
        if (supersedes.isPresent() != version > 1) {
            throw new InvalidValue("only a revision supersedes a plan");
        }
    }
}
