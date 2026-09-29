package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Stock;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ResourceRequirements(
        Set<CertificationId> certifications,
        VehicleRequirement vehicle,
        InstrumentRequirement instrument,
        Map<ConsumableId, Stock> estimatedConsumption
) {
    public ResourceRequirements {
        certifications = Set.copyOf(certifications);
        Objects.requireNonNull(vehicle, "vehicle requirement");
        Objects.requireNonNull(instrument, "instrument requirement");
        estimatedConsumption = Map.copyOf(estimatedConsumption);
    }
}
