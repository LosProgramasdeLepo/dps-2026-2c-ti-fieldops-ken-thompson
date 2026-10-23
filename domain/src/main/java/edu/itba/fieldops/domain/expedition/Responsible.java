package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;

import java.util.Objects;

public final class Responsible {
    private final ExpeditionId expeditionId;
    private final PersonId personId;

    Responsible(Expedition expedition, PersonId personId) {
        this.expeditionId = Objects.requireNonNull(expedition, "expedition").id();
        this.personId = Objects.requireNonNull(personId, "responsible");
    }

    public PersonId personId() {
        return personId;
    }

    boolean belongsTo(Expedition expedition) {
        return expeditionId.equals(Objects.requireNonNull(expedition, "expedition").id())
                && expedition.charter().isResponsible(personId);
    }
}
