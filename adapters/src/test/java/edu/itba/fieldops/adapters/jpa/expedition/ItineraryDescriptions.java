package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.itinerary.ResourceRequirements;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ItineraryDescriptions {
    private ItineraryDescriptions() {
    }

    static List<Object> describe(List<ItineraryItem> items) {
        return items.stream().map(ItineraryDescriptions::describe).toList();
    }

    private static Object describe(ItineraryItem item) {
        return switch (item) {
            case Activity activity -> new ActivityView(
                    activity.id(),
                    activity.name(),
                    activity.estimatedDuration(),
                    activity.risk(),
                    activity.estimatedConsumption(),
                    activity.requirements(),
                    activity.zone(),
                    activity.window(),
                    activity.predecessors()
            );
            case ActivityBlock block -> new BlockView(block.arrangement(), describe(block.parts()));
        };
    }

    private record ActivityView(
            ActivityId id,
            String name,
            Duration estimatedDuration,
            RiskLevel risk,
            Map<ConsumableId, Stock> estimatedConsumption,
            ResourceRequirements requirements,
            WorkZone zone,
            TimePeriod window,
            Set<ActivityId> predecessors
    ) {
    }

    private record BlockView(ActivityBlock.Arrangement arrangement, List<Object> parts) {
    }
}
