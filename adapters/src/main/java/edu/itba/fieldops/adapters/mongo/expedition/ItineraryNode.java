package edu.itba.fieldops.adapters.mongo.expedition;

import edu.itba.fieldops.adapters.mongo.PeriodDocument;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.itinerary.ResourceRequirements;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

record ItineraryNode(String arrangement, ActivityEntry activity, List<ItineraryNode> parts) {
    static ItineraryNode of(ItineraryItem item) {
        return switch (item) {
            case Activity activity -> new ItineraryNode(null, ActivityEntry.of(activity), List.of());
            case ActivityBlock block -> new ItineraryNode(
                    block.arrangement().name(),
                    null,
                    block.parts().stream().map(ItineraryNode::of).toList()
            );
        };
    }

    ItineraryItem toDomain() {
        if (activity != null) {
            return activity.toDomain();
        }
        List<ItineraryItem> items = parts.stream().map(ItineraryNode::toDomain).toList();
        ItineraryItem[] rest = items.subList(2, items.size()).toArray(ItineraryItem[]::new);
        return ActivityBlock.Arrangement.valueOf(arrangement) == ActivityBlock.Arrangement.SEQUENTIAL
                ? ActivityBlock.sequential(items.getFirst(), items.get(1), rest)
                : ActivityBlock.parallel(items.getFirst(), items.get(1), rest);
    }

    record ActivityEntry(
            String id,
            String name,
            String estimatedDuration,
            String risk,
            Map<String, Integer> consumption,
            String zone,
            PeriodDocument window,
            List<String> predecessors,
            Requirements requirements
    ) {
        static ActivityEntry of(Activity activity) {
            return new ActivityEntry(
                    activity.id().value().toString(),
                    activity.name().value(),
                    activity.estimatedDuration().value().toString(),
                    activity.risk().name(),
                    activity.estimatedConsumption().entrySet().stream().collect(Collectors.toMap(
                            entry -> entry.getKey().value().toString(),
                            entry -> entry.getValue().amount()
                    )),
                    activity.zone().name(),
                    PeriodDocument.of(activity.window()),
                    activity.predecessors().stream().map(predecessor -> predecessor.value().toString()).toList(),
                    Requirements.of(activity.requirements())
            );
        }

        Activity toDomain() {
            return Activity.restoring(requirements.toDomain())
                    .named(new ActivityId(UUID.fromString(id)), new ActivityName(name))
                    .estimated(new EstimatedDuration(Duration.parse(estimatedDuration)), RiskLevel.valueOf(risk))
                    .consuming(consumption.entrySet().stream().collect(Collectors.toMap(
                            entry -> new ConsumableId(UUID.fromString(entry.getKey())),
                            entry -> new Consumption(entry.getValue())
                    )))
                    .in(new WorkZone(zone), window.toDomain())
                    .after(predecessors.stream().map(UUID::fromString).map(ActivityId::new).collect(Collectors.toSet()))
                    .build();
        }
    }

    record Requirements(
            List<String> certifications,
            List<String> heldByEveryone,
            List<String> instruments,
            List<String> specialPermits,
            int vehicles
    ) {
        static Requirements of(ResourceRequirements requirements) {
            return new Requirements(
                    requirements.certifications().stream().map(id -> id.value().toString()).toList(),
                    requirements.heldByEveryone().stream().map(id -> id.value().toString()).toList(),
                    requirements.instruments().stream().map(InstrumentKind::name).toList(),
                    requirements.specialPermits().stream().map(PermitKind::name).toList(),
                    requirements.vehicles()
            );
        }

        ResourceRequirements toDomain() {
            return new ResourceRequirements(
                    certifications.stream().map(UUID::fromString).map(CertificationId::new).collect(Collectors.toSet()),
                    heldByEveryone.stream().map(UUID::fromString).map(CertificationId::new).collect(Collectors.toSet()),
                    instruments.stream().map(InstrumentKind::new).collect(Collectors.toSet()),
                    specialPermits.stream().map(PermitKind::new).collect(Collectors.toSet()),
                    vehicles
            );
        }
    }
}
