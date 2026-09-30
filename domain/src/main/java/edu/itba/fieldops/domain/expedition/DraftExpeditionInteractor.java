package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.expedition.usecase.DraftExpedition;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.List;
import java.util.Objects;

public final class DraftExpeditionInteractor implements DraftExpedition {
    private final ExpeditionRepository plans;
    private final People people;

    public DraftExpeditionInteractor(ExpeditionRepository plans, People people) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.people = Objects.requireNonNull(people, "people");
    }

    @Override
    public ExpeditionId draft(
            List<Objective> objectives,
            TimePeriod period,
            List<WorkZone> zones,
            List<PersonId> responsibles,
            List<Restriction> restrictions
    ) {
        Expedition expedition = Expedition.draft(plans.nextId(), objectives, period, zones, responsibles, restrictions);
        for (PersonId responsible : expedition.responsibles()) {
            if (people.person(responsible).isEmpty()) {
                throw new InvalidValue("unknown responsible: " + responsible);
            }
        }
        plans.save(expedition);
        return expedition.id();
    }
}
