package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.ActivityNode;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.ActivityRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.BlockNode;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.CampActivity;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.DiveActivity;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.ItineraryNode;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.MeasurementActivity;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.NightActivity;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.SamplingActivity;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.TransitActivity;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ActivityResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.BlockResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ItineraryItemResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.RequirementsResponse;
import edu.itba.fieldops.api.json.Consumptions;
import edu.itba.fieldops.api.json.Periods;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.itinerary.ResourceRequirements;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class ItineraryMapping {
    private ItineraryMapping() {
    }

    static Activity activity(ActivityRequest request, ActivityId id) {
        Activity.Builder builder = switch (request) {
            case SamplingActivity sampling -> Activity.sampling(new CertificationId(sampling.certification()));
            case MeasurementActivity measurement -> Activity.measurement(
                    new CertificationId(measurement.certification()),
                    new InstrumentKind(measurement.instrument())
            );
            case TransitActivity ignored -> Activity.transit();
            case NightActivity night -> Activity.night(new CertificationId(night.certification()));
            case DiveActivity dive -> Activity.dive(new CertificationId(dive.certification()));
            case CampActivity ignored -> Activity.camp();
        };
        return builder
                .named(id, new ActivityName(request.name()))
                .estimated(new EstimatedDuration(request.estimatedDuration()), request.risk())
                .consuming(Consumptions.toConsumption(request.consumption()))
                .in(new WorkZone(request.zone()), Periods.toPeriod(request.window()))
                .after(predecessors(request.predecessors()))
                .build();
    }

    static ActivityBlock block(BlockNode request, Supplier<ActivityId> ids) {
        List<ItineraryItem> parts = request.parts().stream().map(part -> item(part, ids)).toList();
        ItineraryItem[] rest = parts.subList(2, parts.size()).toArray(ItineraryItem[]::new);
        return request.arrangement() == ActivityBlock.Arrangement.SEQUENTIAL
                ? ActivityBlock.sequential(parts.getFirst(), parts.get(1), rest)
                : ActivityBlock.parallel(parts.getFirst(), parts.get(1), rest);
    }

    static ActivityResponse activity(Activity activity) {
        return new ActivityResponse(
                activity.id().value(),
                activity.name().value(),
                activity.estimatedDuration().value().toString(),
                activity.risk().name(),
                Consumptions.toResponse(activity.estimatedConsumption()),
                activity.zone().name(),
                Periods.toResponse(activity.window()),
                activity.predecessors().stream().map(ActivityId::value).sorted().toList(),
                requirements(activity.requirements())
        );
    }

    static BlockResponse block(ActivityBlock block) {
        return new BlockResponse(block.arrangement().name(), block.parts().stream().map(ItineraryMapping::item).toList());
    }

    static ItineraryItemResponse item(ItineraryItem item) {
        return switch (item) {
            case Activity activity -> activity(activity);
            case ActivityBlock block -> block(block);
        };
    }

    private static ItineraryItem item(ItineraryNode node, Supplier<ActivityId> ids) {
        return switch (node) {
            case ActivityNode activity -> activity(activity.activity(), ids.get());
            case BlockNode block -> block(block, ids);
        };
    }

    private static RequirementsResponse requirements(ResourceRequirements requirements) {
        return new RequirementsResponse(
                requirements.certifications().stream().map(CertificationId::value).sorted().toList(),
                requirements.heldByEveryone().stream().map(CertificationId::value).sorted().toList(),
                requirements.instruments().stream().map(InstrumentKind::name).sorted().toList(),
                requirements.specialPermits().stream().map(PermitKind::name).sorted().toList(),
                requirements.vehicles()
        );
    }

    private static Set<ActivityId> predecessors(Set<UUID> ids) {
        return ids.stream().map(ActivityId::new).collect(Collectors.toSet());
    }
}
