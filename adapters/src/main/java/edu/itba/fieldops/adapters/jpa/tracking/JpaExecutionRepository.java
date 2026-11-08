package edu.itba.fieldops.adapters.jpa.tracking;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.SQL)
@Transactional(readOnly = true)
public class JpaExecutionRepository implements ExecutionRepository {
    private final RunJpaRepository runs;

    public JpaExecutionRepository(RunJpaRepository runs) {
        this.runs = Objects.requireNonNull(runs, "runs");
    }

    @Override
    public IncidentId nextIncidentId() {
        return new IncidentId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(ExpeditionExecution execution) {
        Objects.requireNonNull(execution, "execution");
        runs.save(new RunEntity(execution.state()));
    }

    @Override
    public Optional<ExpeditionExecution> find(ExpeditionId expeditionId) {
        Objects.requireNonNull(expeditionId, "expedition id");
        return runs.findById(expeditionId.value()).map(RunEntity::toDomain);
    }
}
