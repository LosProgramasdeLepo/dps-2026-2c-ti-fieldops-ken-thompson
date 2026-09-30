package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.PermitKind;

import java.util.Set;

public record ResourceRequirements(
        Set<CertificationId> certifications,
        Set<CertificationId> heldByEveryone,
        Set<InstrumentKind> instruments,
        Set<PermitKind> specialPermits,
        int vehicles
) {
    public ResourceRequirements {
        certifications = Set.copyOf(certifications);
        heldByEveryone = Set.copyOf(heldByEveryone);
        instruments = Set.copyOf(instruments);
        specialPermits = Set.copyOf(specialPermits);
        if (vehicles < 0) {
            throw new InvalidValue("required vehicles must not be negative");
        }
    }

    public boolean needsCertifiedPersonnel() {
        return !certifications.isEmpty() || !heldByEveryone.isEmpty();
    }
}
