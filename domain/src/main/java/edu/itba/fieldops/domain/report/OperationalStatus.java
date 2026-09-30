package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;

public enum OperationalStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    IN_PROGRESS,
    SUSPENDED,
    FINISHED,
    SUPERSEDED;

    static OperationalStatus of(Expedition expedition, ExpeditionExecution execution) {
        if (execution != null) {
            return switch (execution.status()) {
                case IN_PROGRESS -> IN_PROGRESS;
                case SUSPENDED -> SUSPENDED;
                case FINISHED -> FINISHED;
            };
        }
        return switch (expedition.status()) {
            case DRAFT -> DRAFT;
            case IN_REVIEW -> IN_REVIEW;
            case APPROVED -> APPROVED;
            case SUPERSEDED -> SUPERSEDED;
        };
    }
}
