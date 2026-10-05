package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.TimePeriod;

public interface Bookable {
    boolean availableDuring(TimePeriod period);
}
