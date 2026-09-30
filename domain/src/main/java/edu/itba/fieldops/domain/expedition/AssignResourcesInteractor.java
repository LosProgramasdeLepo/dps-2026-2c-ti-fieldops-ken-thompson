package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.expedition.usecase.AssignResources;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.ExecutionRepository;

import java.util.List;
import java.util.Objects;

public final class AssignResourcesInteractor implements AssignResources {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final Catalogs catalogs;
    private final AssignmentSuggester suggester;

    public AssignResourcesInteractor(
            ExpeditionRepository plans,
            ExecutionRepository executions,
            Catalogs catalogs,
            AssignmentSuggester suggester
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.executions = Objects.requireNonNull(executions, "executions");
        this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
        this.suggester = Objects.requireNonNull(suggester, "suggester");
    }

    @Override
    public void addAssignment(ExpeditionId expeditionId, Assignment assignment) {
        Objects.requireNonNull(assignment, "assignment");
        assignment.unknownIn(catalogs).ifPresent(label -> {
            throw new InvalidAssignment("unknown " + label);
        });
        Expedition expedition = require(expeditionId);
        expedition.addAssignment(assignment);
        plans.save(expedition);
    }

    @Override
    public void addPermit(ExpeditionId expeditionId, PermitId permitId) {
        if (catalogs.permits().permit(Objects.requireNonNull(permitId, "permit id")).isEmpty()) {
            throw new InvalidValue("unknown permit: " + permitId);
        }
        Expedition expedition = require(expeditionId);
        expedition.addPermit(permitId);
        plans.save(expedition);
    }

    @Override
    public List<Assignment> suggest(ExpeditionId expeditionId) {
        Expedition expedition = require(expeditionId);
        return suggester.suggest(expedition, catalogs.bookable(), Peers.around(expedition, plans, executions));
    }

    private Expedition require(ExpeditionId expeditionId) {
        return plans.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidValue("unknown expedition: " + expeditionId));
    }
}
