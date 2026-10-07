package edu.itba.fieldops.api.json;

import java.util.List;

public record AvailabilityResponse(List<PeriodResponse> periods) {
}
