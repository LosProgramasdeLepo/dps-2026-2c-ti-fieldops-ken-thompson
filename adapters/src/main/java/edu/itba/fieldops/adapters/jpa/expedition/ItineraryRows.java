package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.itinerary.ResourceRequirements;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

record ItineraryRows(
        List<StoredNode> nodes,
        Set<StoredActivity> activities,
        Set<StoredRequirement> certifications,
        Set<StoredKind> instrumentKinds,
        Set<StoredKind> permitKinds,
        Set<StoredConsumption> consumption,
        Set<StoredPredecessor> predecessors
) {
    static ItineraryRows of(List<ItineraryItem> items) {
        ItineraryRows rows = new ItineraryRows(
                new ArrayList<>(),
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>()
        );
        items.forEach(item -> rows.add(item, Optional.empty()));
        return rows;
    }

    List<ItineraryItem> items() {
        Map<UUID, StoredActivity> stored = activities.stream()
                .collect(Collectors.toMap(StoredActivity::activityId, Function.identity()));
        return childrenOf(Optional.empty()).stream().map(position -> item(position, stored)).toList();
    }

    private void add(ItineraryItem item, Optional<Integer> parent) {
        int position = nodes.size();
        switch (item) {
            case Activity activity -> {
                nodes.add(StoredNode.leaf(parent, activity.id().value()));
                add(activity);
            }
            case ActivityBlock block -> {
                nodes.add(StoredNode.block(parent, block.arrangement().name()));
                block.parts().forEach(part -> add(part, Optional.of(position)));
            }
        }
    }

    private void add(Activity activity) {
        UUID id = activity.id().value();
        ResourceRequirements requirements = activity.requirements();
        activities.add(new StoredActivity(
                id,
                activity.name(),
                activity.estimatedDuration(),
                activity.risk().name(),
                activity.zone().name(),
                activity.window().start(),
                activity.window().end(),
                requirements.vehicles()
        ));
        requirements.certifications().forEach(certification -> certifications.add(
                new StoredRequirement(id, certification.value(), StoredRequirement.SOMEONE)
        ));
        requirements.heldByEveryone().forEach(certification -> certifications.add(
                new StoredRequirement(id, certification.value(), StoredRequirement.EVERYONE)
        ));
        requirements.instruments().forEach(kind -> instrumentKinds.add(new StoredKind(id, kind.name())));
        requirements.specialPermits().forEach(kind -> permitKinds.add(new StoredKind(id, kind.name())));
        activity.estimatedConsumption().forEach((consumable, quantity) -> consumption.add(
                new StoredConsumption(id, consumable.value(), quantity.amount())
        ));
        activity.predecessors().forEach(predecessor -> predecessors.add(new StoredPredecessor(id, predecessor.value())));
    }

    private ItineraryItem item(int position, Map<UUID, StoredActivity> stored) {
        StoredNode node = nodes.get(position);
        return node.activity()
                .<ItineraryItem>map(activityId -> activity(stored.get(activityId)))
                .orElseGet(() -> block(position, node.arrangement(), stored));
    }

    private ActivityBlock block(int position, String arrangement, Map<UUID, StoredActivity> stored) {
        List<ItineraryItem> parts = childrenOf(Optional.of(position)).stream()
                .map(child -> item(child, stored))
                .toList();
        ItineraryItem[] rest = parts.subList(2, parts.size()).toArray(ItineraryItem[]::new);
        return ActivityBlock.Arrangement.valueOf(arrangement) == ActivityBlock.Arrangement.SEQUENTIAL
                ? ActivityBlock.sequential(parts.getFirst(), parts.get(1), rest)
                : ActivityBlock.parallel(parts.getFirst(), parts.get(1), rest);
    }

    private Activity activity(StoredActivity stored) {
        UUID id = stored.activityId();
        return Activity.restoring(requirementsOf(stored))
                .named(new ActivityId(id), stored.name())
                .estimated(stored.estimatedDuration(), RiskLevel.valueOf(stored.risk()))
                .consuming(consumption.stream()
                        .filter(row -> row.activityId().equals(id))
                        .collect(Collectors.toMap(
                                row -> new ConsumableId(row.consumableId()),
                                row -> new Stock(row.quantity())
                        )))
                .in(new WorkZone(stored.zone()), new TimePeriod(stored.windowStart(), stored.windowEnd()))
                .after(predecessors.stream()
                        .filter(row -> row.activityId().equals(id))
                        .map(row -> new ActivityId(row.predecessorId()))
                        .collect(Collectors.toSet()))
                .build();
    }

    private ResourceRequirements requirementsOf(StoredActivity stored) {
        UUID id = stored.activityId();
        return new ResourceRequirements(
                certificationsOf(id, StoredRequirement.SOMEONE),
                certificationsOf(id, StoredRequirement.EVERYONE),
                instrumentKinds.stream()
                        .filter(row -> row.activityId().equals(id))
                        .map(row -> new InstrumentKind(row.kind()))
                        .collect(Collectors.toSet()),
                permitKinds.stream()
                        .filter(row -> row.activityId().equals(id))
                        .map(row -> new PermitKind(row.kind()))
                        .collect(Collectors.toSet()),
                stored.requiredVehicles()
        );
    }

    private Set<CertificationId> certificationsOf(UUID activityId, String scope) {
        return certifications.stream()
                .filter(row -> row.activityId().equals(activityId) && row.scope().equals(scope))
                .map(row -> new CertificationId(row.certificationId()))
                .collect(Collectors.toSet());
    }

    private List<Integer> childrenOf(Optional<Integer> parent) {
        return IntStream.range(0, nodes.size())
                .filter(position -> nodes.get(position).enclosingBlock().equals(parent))
                .boxed()
                .toList();
    }
}
