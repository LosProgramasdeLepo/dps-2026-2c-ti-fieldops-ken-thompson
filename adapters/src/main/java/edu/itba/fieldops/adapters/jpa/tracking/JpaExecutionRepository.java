package edu.itba.fieldops.adapters.jpa.tracking;

import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.usecase.expedition.ExecutionRepository;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Objects;
import java.util.Optional;

public final class JpaExecutionRepository implements ExecutionRepository {
    private final RunJpaRepository runs;
    private final TransactionOperations transactions;

    public JpaExecutionRepository(RunJpaRepository runs, TransactionOperations transactions) {
        this.runs = Objects.requireNonNull(runs, "runs");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public void save(ExpeditionExecution execution) {
        Objects.requireNonNull(execution, "execution");
        transactions.executeWithoutResult(status -> runs.save(new RunEntity(execution.state())));
    }

    @Override
    public Optional<ExpeditionExecution> find(ExpeditionId expeditionId) {
        Objects.requireNonNull(expeditionId, "expedition id");
        return transactions.execute(status -> runs.findById(expeditionId.value()).map(RunEntity::toDomain));
    }
}
