package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Occurrence;

import java.util.Objects;
import java.util.Set;

public final class ActivityStart {
    private final ActivityId activityId;
    private final Occurrence at;
    private final Set<ActivityId> predecessors;
    private final Set<ExpeditionId> lineage;

    ActivityStart(
            ActivityId activityId,
            Occurrence at,
            Set<ActivityId> predecessors,
            Set<ExpeditionId> lineage
    ) {
        this.activityId = Objects.requireNonNull(activityId, "activity id");
        this.at = Objects.requireNonNull(at, "occurrence");
        this.predecessors = Set.copyOf(predecessors);
        this.lineage = Set.copyOf(lineage);
    }

    ActivityId activityId() {
        return activityId;
    }

    Occurrence at() {
        return at;
    }

    Set<ActivityId> predecessors() {
        return predecessors;
    }

    boolean covers(ExpeditionId expeditionId) {
        return lineage.contains(expeditionId);
    }
}
