package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.CertificationId;

import java.util.Objects;
import java.util.Set;

public record ResourceRequirements(
        Set<CertificationId> certifications,
        Set<CertificationId> heldByEveryone,
        VehicleRequirement vehicle,
        InstrumentRequirement instrument,
        NightPermit nightPermit
) {
    public ResourceRequirements {
        certifications = Set.copyOf(certifications);
        heldByEveryone = Set.copyOf(heldByEveryone);
        Objects.requireNonNull(vehicle, "vehicle requirement");
        Objects.requireNonNull(instrument, "instrument requirement");
        Objects.requireNonNull(nightPermit, "night permit");
    }
}
