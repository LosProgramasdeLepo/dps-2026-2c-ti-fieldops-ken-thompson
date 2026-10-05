package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.AssignmentSuggester;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.InvalidAssignment;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.shared.UnknownResource;

import java.util.List;
import java.util.Objects;

public final class AssignResourcesInteractor implements AssignResources {
    private final ExpeditionRepository plans;
    private final Catalogs catalogs;
    private final PlanningContexts contexts;
    private final AssignmentSuggester suggester;

    public AssignResourcesInteractor(
            ExpeditionRepository plans,
            ExecutionRepository executions,
            Catalogs catalogs,
            AssignmentSuggester suggester
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
        this.contexts = new PlanningContexts(plans, executions, catalogs);
        this.suggester = Objects.requireNonNull(suggester, "suggester");
    }

    @Override
    public void addAssignment(ExpeditionId expeditionId, Assignment assignment) {
        Objects.requireNonNull(assignment, "assignment");
        assignment.unknownIn(catalogs).ifPresent(label -> {
            throw new InvalidAssignment("unknown " + label);
        });
        Expedition expedition = plans.require(expeditionId);
        expedition.addAssignment(assignment);
        plans.save(expedition);
    }

    @Override
    public void addPermit(ExpeditionId expeditionId, PermitId permitId) {
        if (catalogs.permits().permit(Objects.requireNonNull(permitId, "permit id")).isEmpty()) {
            throw new UnknownResource("permit", permitId.value().toString());
        }
        Expedition expedition = plans.require(expeditionId);
        expedition.addPermit(permitId);
        plans.save(expedition);
    }

    @Override
    public List<Assignment> suggest(ExpeditionId expeditionId) {
        return suggester.suggest(contexts.around(plans.require(expeditionId)));
    }
}
