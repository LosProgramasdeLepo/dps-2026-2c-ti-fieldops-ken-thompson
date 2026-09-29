package edu.itba.fieldops.domain.expedition;

public enum ExpeditionStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED;

    public boolean isEditable() {
        return this == DRAFT || this == IN_REVIEW;
    }

    public boolean occupiesResources() {
        return this == IN_REVIEW || this == APPROVED;
    }

    public boolean hasBeenApproved() {
        return this == APPROVED;
    }

    public boolean canReturnToDraft() {
        return this == IN_REVIEW || this == APPROVED;
    }
}
