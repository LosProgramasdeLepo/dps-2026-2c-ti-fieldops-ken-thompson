package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.TimePeriod;

public interface BookableAssignment extends Assignment {
    TemporalBooking booking(TimePeriod window);
}
