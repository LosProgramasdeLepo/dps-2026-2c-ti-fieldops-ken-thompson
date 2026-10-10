package edu.itba.fieldops.domain.support;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionState;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;

public final class StoredPlans {
    private StoredPlans() {
    }

    public static Expedition approved(Expedition inReview) {
        ExpeditionState state = inReview.state();
        return Expedition.restore(new ExpeditionState(
                state.id(),
                state.version(),
                state.supersedes(),
                ExpeditionStatus.APPROVED,
                state.charter(),
                state.items(),
                state.assignments(),
                state.permits(),
                state.acceptedWarnings()
        ));
    }
}
