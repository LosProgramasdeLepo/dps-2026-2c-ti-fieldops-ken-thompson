package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.ActivityId;

import java.util.Optional;

public interface Assignment {
    ActivityId activityId();

    Optional<String> unknownIn(Catalogs catalogs);

    void fileInto(Assignments assignments);

    void withdrawFrom(Assignments assignments);
}
