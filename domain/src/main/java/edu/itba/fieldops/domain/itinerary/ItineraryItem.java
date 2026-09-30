package edu.itba.fieldops.domain.itinerary;

import java.time.Duration;
import java.util.List;

public sealed interface ItineraryItem permits Activity, ActivityBlock {
    Duration estimatedDuration();

    List<Activity> activities();
}
