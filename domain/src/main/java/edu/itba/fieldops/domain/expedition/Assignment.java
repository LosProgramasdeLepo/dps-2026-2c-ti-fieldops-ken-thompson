package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Optional;

public interface Assignment {
    ActivityId activityId();

    Optional<TemporalBooking> booking(TimePeriod window);

    Optional<String> unknownIn(Catalogs catalogs);

    void fileInto(Assignments assignments);

    boolean withdrawFrom(Assignments assignments);
}
