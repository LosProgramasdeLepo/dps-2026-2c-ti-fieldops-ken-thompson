package edu.itba.fieldops.adapters.mongo.tracking;

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
@Profile(PersistenceProfiles.MONGO)
@Transactional(readOnly = true)
public class MongoExecutionRepository implements ExecutionRepository {
    private final RunMongoRepository runs;

    MongoExecutionRepository(RunMongoRepository runs) {
        this.runs = Objects.requireNonNull(runs, "runs");
    }

    @Override
    public IncidentId nextIncidentId() {
        return new IncidentId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(ExpeditionExecution execution) {
        runs.save(RunDocument.of(Objects.requireNonNull(execution, "execution").state()));
    }

    @Override
    public Optional<ExpeditionExecution> find(ExpeditionId expeditionId) {
        return runs.findById(Objects.requireNonNull(expeditionId, "expedition id").value().toString()).map(RunDocument::toDomain);
    }
}
