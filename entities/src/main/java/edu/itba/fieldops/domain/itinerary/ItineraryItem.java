package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.identity.ActivityId;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public sealed interface ItineraryItem permits Activity, ActivityBlock {
    Duration duration(Function<Activity, Duration> leafDuration);

    List<Activity> activities();

    List<ActivityBlock> blocks();

    Map<ActivityId, Set<ActivityId>> precedence();

    ItineraryItem replacing(Activity updated);

    Optional<ItineraryItem> without(ActivityId activityId);
}
