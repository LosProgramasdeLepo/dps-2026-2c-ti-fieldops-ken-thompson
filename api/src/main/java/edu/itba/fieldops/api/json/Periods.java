package edu.itba.fieldops.api.json;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.shared.TimePeriod;

public final class Periods {
    private Periods() {
    }

    public static TimePeriod toPeriod(PeriodRequest request) {
        return new TimePeriod(request.start(), request.end());
    }

    public static PeriodResponse toResponse(TimePeriod period) {
        return new PeriodResponse(period.start(), period.end());
    }

    public static Availability toAvailability(AvailabilityRequest request) {
        return new Availability(request.periods().stream().map(Periods::toPeriod).toList());
    }
}
