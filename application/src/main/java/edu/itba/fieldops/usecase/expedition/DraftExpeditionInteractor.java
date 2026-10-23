package edu.itba.fieldops.usecase.expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.usecase.expedition.DraftExpedition;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;

import java.util.ArrayList;
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
    public ExpeditionId draft(ExpeditionCharter charter) {
        Objects.requireNonNull(charter, "charter");
        List<Person> known = new ArrayList<>();
        for (PersonId responsible : charter.responsibles()) {
            people.person(responsible).ifPresent(known::add);
        }
        Expedition expedition = Expedition.draft(plans.nextId(), charter, known);
        plans.save(expedition);
        return expedition.id();
    }
}
