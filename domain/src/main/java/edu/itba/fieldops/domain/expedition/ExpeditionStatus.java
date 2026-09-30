package edu.itba.fieldops.domain.expedition;

public enum ExpeditionStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    SUPERSEDED;

    public boolean occupiesResources() {
        return this == IN_REVIEW || this == APPROVED;
    }
}
