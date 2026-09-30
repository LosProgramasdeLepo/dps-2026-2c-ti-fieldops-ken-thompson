package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.expedition.usecase.DraftExpedition;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.Objects;

public final class DraftExpeditionInteractor implements DraftExpedition {
    private final ExpeditionRepository plans;
    private final People people;

    public DraftExpeditionInteractor(ExpeditionRepository plans, People people) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.people = Objects.requireNonNull(people, "people");
    }

    @Override
    public ExpeditionId draft(ExpeditionCharter charter) {
        Objects.requireNonNull(charter, "charter");
        for (PersonId responsible : charter.responsibles()) {
            if (people.person(responsible).isEmpty()) {
                throw new InvalidValue("unknown responsible: " + responsible);
            }
        }
        Expedition expedition = Expedition.draft(plans.nextId(), charter);
        plans.save(expedition);
        return expedition.id();
    }
}
