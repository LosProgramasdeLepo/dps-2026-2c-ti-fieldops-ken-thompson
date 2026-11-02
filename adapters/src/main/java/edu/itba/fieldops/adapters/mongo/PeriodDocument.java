package edu.itba.fieldops.adapters.mongo;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.time.Instant;
import java.util.List;

public record PeriodDocument(Instant start, Instant end) {
    public static PeriodDocument of(TimePeriod period) {
        return new PeriodDocument(period.start(), period.end());
    }

    public static List<PeriodDocument> of(Availability availability) {
        return availability.periods().stream().map(PeriodDocument::of).toList();
    }

    public static Availability availability(List<PeriodDocument> periods) {
        return new Availability(periods.stream().map(PeriodDocument::toDomain).toList());
    }

    public TimePeriod toDomain() {
        return new TimePeriod(start, end);
    }
}
