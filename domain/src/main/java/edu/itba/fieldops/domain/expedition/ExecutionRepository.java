package edu.itba.fieldops.domain.expedition;

public interface ExecutionRepository extends Runs {
    void save(ExpeditionExecution execution);
}
