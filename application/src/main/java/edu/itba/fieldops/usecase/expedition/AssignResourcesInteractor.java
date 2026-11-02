package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.AssignmentSuggester;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.PlanningContexts;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.usecase.expedition.AssignResources;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class AssignResourcesInteractor implements AssignResources {
    private final ExpeditionRepository plans;
    private final Catalogs catalogs;
    private final PlanningContexts contexts;
    private final AssignmentSuggester suggester;

    public AssignResourcesInteractor(
            ExpeditionRepository plans,
            Catalogs catalogs,
            PlanningContexts contexts,
            AssignmentSuggester suggester
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
        this.contexts = Objects.requireNonNull(contexts, "planning contexts");
        this.suggester = Objects.requireNonNull(suggester, "suggester");
    }

    @Override
    public void addAssignment(ExpeditionId expeditionId, Assignment assignment) {
        Expedition expedition = plans.require(expeditionId);
        expedition.addAssignment(assignment, catalogs);
        plans.save(expedition);
    }

    @Override
    public void addPermit(ExpeditionId expeditionId, PermitId permitId) {
        Expedition expedition = plans.require(expeditionId);
        if (expedition.permits().contains(permitId)) {
            return;
        }
        expedition.addPermit(permitId, catalogs.permits());
        plans.save(expedition);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> suggest(ExpeditionId expeditionId) {
        return suggester.suggest(contexts.around(plans.require(expeditionId)));
    }
}
