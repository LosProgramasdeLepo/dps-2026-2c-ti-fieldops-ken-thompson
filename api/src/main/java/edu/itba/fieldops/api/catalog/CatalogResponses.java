package edu.itba.fieldops.api.catalog;

import edu.itba.fieldops.api.json.AvailabilityResponse;
import edu.itba.fieldops.api.json.PeriodResponse;

import java.util.List;
import java.util.UUID;

public final class CatalogResponses {
    private CatalogResponses() {
    }

    public record CertificationResponse(UUID id, String name) {
    }

    public record PersonResponse(
            UUID id,
            String name,
            List<CertificationResponse> certifications,
            AvailabilityResponse availability
    ) {
    }

    public record VehicleResponse(UUID id, int capacity, AvailabilityResponse availability) {
    }

    public record InstrumentResponse(UUID id, String kind, AvailabilityResponse availability) {
    }

    public record ConsumableResponse(UUID id, String name, int stock) {
    }

    public record PermitResponse(UUID id, String kind, String zone, PeriodResponse validity) {
    }
}
