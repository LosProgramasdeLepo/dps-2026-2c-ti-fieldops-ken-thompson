package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Optional;

public sealed interface Assignment permits PersonAssignment, VehicleAssignment, InstrumentAssignment, ConsumableAssignment {
    ActivityId activityId();

    Optional<TemporalBooking> booking(TimePeriod window);

    Optional<String> unknownIn(Catalog catalog);
}
