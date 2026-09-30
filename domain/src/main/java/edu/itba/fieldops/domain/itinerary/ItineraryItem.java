package edu.itba.fieldops.domain.itinerary;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

public sealed interface ItineraryItem permits Activity, ActivityBlock {
    Duration duration(Function<Activity, Duration> leafDuration);

    List<Activity> activities();
}
