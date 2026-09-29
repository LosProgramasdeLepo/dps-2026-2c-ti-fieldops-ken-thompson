package edu.itba.fieldops.domain.expedition;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum ExpeditionStatus {
    DRAFT(Capability.EDITABLE),
    IN_REVIEW(Capability.EDITABLE, Capability.OCCUPIES_RESOURCES, Capability.RETURNS_TO_DRAFT),
    APPROVED(Capability.OCCUPIES_RESOURCES, Capability.APPROVED, Capability.RETURNS_TO_DRAFT);

    private final Set<Capability> capabilities;

    ExpeditionStatus(Capability... capabilities) {
        this.capabilities = EnumSet.copyOf(List.of(capabilities));
    }

    public boolean isEditable() {
        return capabilities.contains(Capability.EDITABLE);
    }

    public boolean occupiesResources() {
        return capabilities.contains(Capability.OCCUPIES_RESOURCES);
    }

    public boolean hasBeenApproved() {
        return capabilities.contains(Capability.APPROVED);
    }

    public boolean canReturnToDraft() {
        return capabilities.contains(Capability.RETURNS_TO_DRAFT);
    }

    private enum Capability {
        EDITABLE,
        OCCUPIES_RESOURCES,
        APPROVED,
        RETURNS_TO_DRAFT
    }
}
