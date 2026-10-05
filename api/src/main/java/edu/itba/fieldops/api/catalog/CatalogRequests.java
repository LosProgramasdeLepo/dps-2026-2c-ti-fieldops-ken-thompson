package edu.itba.fieldops.api.catalog;

import edu.itba.fieldops.api.json.AvailabilityRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class CatalogRequests {
    private CatalogRequests() {
    }

    public record NameRequest(@NotBlank @Size(max = 200) String name) {
    }

    public record RegisterPersonRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull @Size(max = 50) List<@NotNull UUID> certifications,
            @NotNull @Valid AvailabilityRequest availability
    ) {
    }

    public record RegisterVehicleRequest(
            @NotNull @Min(0) Integer capacity,
            @NotNull @Valid AvailabilityRequest availability
    ) {
    }

    public record RegisterInstrumentRequest(
            @NotBlank @Size(max = 100) String kind,
            @NotNull @Valid AvailabilityRequest availability
    ) {
    }

    public record RegisterConsumableRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull @Min(0) Integer stock
    ) {
    }

    public record RegisterPermitRequest(
            @NotBlank @Size(max = 100) String kind,
            @NotBlank @Size(max = 200) String zone,
            @NotNull @Valid edu.itba.fieldops.api.json.PeriodRequest validity
    ) {
    }

    public record AvailabilityChangeRequest(@NotNull @Valid AvailabilityRequest availability) {
    }

    public record StockChangeRequest(@NotNull @Min(0) Integer stock) {
    }
}
