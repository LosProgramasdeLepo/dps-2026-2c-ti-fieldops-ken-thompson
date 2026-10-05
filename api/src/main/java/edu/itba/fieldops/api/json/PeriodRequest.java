package edu.itba.fieldops.api.json;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PeriodRequest(@NotNull Instant start, @NotNull Instant end) {
}
