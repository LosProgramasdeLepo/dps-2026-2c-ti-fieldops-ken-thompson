package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class AttachedPermits {
    private final List<PermitId> permits = new ArrayList<>();

    void attach(PermitId permitId) {
        if (permits.contains(Objects.requireNonNull(permitId, "permit id"))) {
            throw new InvalidValue("duplicate permit: " + permitId);
        }
        permits.add(permitId);
    }

    List<PermitId> all() {
        return List.copyOf(permits);
    }

    AttachedPermits copy() {
        AttachedPermits copy = new AttachedPermits();
        copy.permits.addAll(permits);
        return copy;
    }
}
