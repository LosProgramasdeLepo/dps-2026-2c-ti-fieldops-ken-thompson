package edu.itba.fieldops.api.json;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AvailabilityRequest(
        @NotNull @Size(max = 50) List<@NotNull @Valid PeriodRequest> periods
) {
}
