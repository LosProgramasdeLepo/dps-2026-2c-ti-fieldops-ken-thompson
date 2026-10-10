package edu.itba.fieldops.adapters.jpa;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.shared.TimePeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.List;

@Embeddable
public record StoredPeriod(
        @Column(name = "period_start", nullable = false) Instant start,
        @Column(name = "period_end", nullable = false) Instant end
) {
    public static StoredPeriod of(TimePeriod period) {
        return new StoredPeriod(period.start(), period.end());
    }

    public static List<StoredPeriod> of(Availability availability) {
        return availability.periods().stream().map(StoredPeriod::of).toList();
    }

    public static Availability availability(List<StoredPeriod> periods) {
        return new Availability(periods.stream().map(StoredPeriod::toDomain).toList());
    }

    public TimePeriod toDomain() {
        return new TimePeriod(start, end);
    }
}
