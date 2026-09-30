package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Bookable;
import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.BookableId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Optional;

public interface BookableAssignment extends Assignment {
    BookableId resourceId();

    Optional<? extends Bookable> resourceIn(BookableResources resources);

    @Override
    default Optional<String> unknownIn(Catalogs catalogs) {
        return resourceIn(catalogs.bookable()).isPresent() ? Optional.empty() : Optional.of(resourceId().label());
    }

    default TemporalBooking booking(TimePeriod window) {
        return new TemporalBooking(this, window);
    }
}
