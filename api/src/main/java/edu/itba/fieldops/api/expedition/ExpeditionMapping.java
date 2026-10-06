package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.AcceptWarningRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.ActivityNode;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.ActivityRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.AssignmentRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.BlockNode;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.CampActivity;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.CharterRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.ConsumableAssignmentRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.DiveActivity;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.InstrumentAssignmentRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.ItineraryNode;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.MeasurementActivity;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.NightActivity;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.PersonAssignmentRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.SamplingActivity;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.TransitActivity;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.VehicleAssignmentRequest;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.AcceptedWarningResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ActivityExecutionResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ActivityResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ActivityResultResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.AssignmentResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.BlockResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.CharterResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ConsumableAssignmentResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.EstimateResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ExpeditionResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ExpeditionSummaryResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.IncidentResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.InstrumentAssignmentResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.IssueResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ItineraryItemResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ObservationResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.PersonAssignmentResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ProposalResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ReportResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.RequirementsResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.RunResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ValidationResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.VehicleAssignmentResponse;
import edu.itba.fieldops.api.json.Periods;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.itinerary.ResourceRequirements;
import edu.itba.fieldops.domain.report.ActivityResult;
import edu.itba.fieldops.domain.report.Estimate;
import edu.itba.fieldops.domain.report.OperationalReport;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.usecase.expedition.PlanSnapshot;
import edu.itba.fieldops.usecase.expedition.ProposalSnapshot;
import edu.itba.fieldops.usecase.expedition.RunSnapshot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ExpeditionMapping {
    private ExpeditionMapping() {
    }

    public static ExpeditionCharter charter(CharterRequest request) {
        return new ExpeditionCharter(
                request.objectives().stream().map(Objective::new).toList(),
                Periods.toPeriod(request.period()),
                request.zones().stream().map(WorkZone::new).toList(),
                request.responsibles().stream().map(PersonId::new).toList(),
                request.restrictions().stream().map(Restriction::new).toList()
        );
    }

    public static Activity activity(ActivityRequest request, ActivityId id) {
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
                .named(id, request.name())
                .estimated(request.estimatedDuration(), request.risk())
                .consuming(consumption(request.consumption()))
                .in(new WorkZone(request.zone()), Periods.toPeriod(request.window()))
                .after(predecessors(request.predecessors()))
                .build();
    }

    public static ActivityBlock block(BlockNode request, Supplier<ActivityId> ids) {
        List<ItineraryItem> parts = request.parts().stream().map(part -> item(part, ids)).toList();
        ItineraryItem[] rest = parts.subList(2, parts.size()).toArray(ItineraryItem[]::new);
        return request.arrangement() == ActivityBlock.Arrangement.SEQUENTIAL
                ? ActivityBlock.sequential(parts.getFirst(), parts.get(1), rest)
                : ActivityBlock.parallel(parts.getFirst(), parts.get(1), rest);
    }

    public static Assignment assignment(AssignmentRequest request) {
        return switch (request) {
            case PersonAssignmentRequest person -> new PersonAssignment(new ActivityId(person.activityId()), new PersonId(person.personId()));
            case VehicleAssignmentRequest vehicle -> new VehicleAssignment(new ActivityId(vehicle.activityId()), new VehicleId(vehicle.vehicleId()));
            case InstrumentAssignmentRequest instrument -> new InstrumentAssignment(
                    new ActivityId(instrument.activityId()),
                    new InstrumentId(instrument.instrumentId())
            );
            case ConsumableAssignmentRequest consumable -> new ConsumableAssignment(
                    new ActivityId(consumable.activityId()),
                    new ConsumableId(consumable.consumableId()),
                    new Stock(consumable.quantity())
            );
        };
    }

    public static AcceptedWarning warning(AcceptWarningRequest request) {
        return new AcceptedWarning(
                new ValidationIssue(request.issue().severity(), request.issue().code(), request.issue().message()),
                request.justification(),
                new PersonId(request.acceptedBy())
        );
    }

    public static ExpeditionResponse expedition(PlanSnapshot snapshot) {
        return new ExpeditionResponse(
                snapshot.id().value(),
                snapshot.version(),
                snapshot.supersedes().map(id -> id.value()).orElse(null),
                snapshot.status().name(),
                charter(snapshot.charter()),
                snapshot.itinerary().stream().map(ExpeditionMapping::item).toList(),
                snapshot.assignments().stream().map(ExpeditionMapping::assignment).toList(),
                snapshot.permits().stream().map(PermitId::value).toList(),
                snapshot.acceptedWarnings().stream().map(ExpeditionMapping::warning).toList()
        );
    }

    public static ExpeditionSummaryResponse summary(PlanSnapshot snapshot) {
        return new ExpeditionSummaryResponse(
                snapshot.id().value(),
                snapshot.version(),
                snapshot.supersedes().map(id -> id.value()).orElse(null),
                snapshot.status().name(),
                charter(snapshot.charter())
        );
    }

    public static BlockResponse block(ActivityBlock block) {
        return (BlockResponse) item(block);
    }

    public static List<AssignmentResponse> assignments(List<Assignment> assignments) {
        return assignments.stream().map(ExpeditionMapping::assignment).toList();
    }

    public static EstimateResponse estimate(Estimate estimate) {
        return new EstimateResponse(estimate.duration().toString(), estimate.risk().name(), amounts(estimate.estimatedConsumption()));
    }

    public static ReportResponse report(OperationalReport report) {
        return new ReportResponse(
                report.status().name(),
                report.plannedActivities(),
                report.startedActivities(),
                report.finishedActivities(),
                report.duration().toString(),
                report.risk().name(),
                amounts(report.consumption()),
                amounts(report.estimatedConsumption()),
                report.incidents().stream().map(ExpeditionMapping::incident).toList(),
                report.observations().stream().map(ExpeditionMapping::observation).toList(),
                report.activityResults().stream().map(ExpeditionMapping::result).toList()
        );
    }

    public static RunResponse run(RunSnapshot snapshot) {
        return new RunResponse(
                snapshot.expeditionId().value(),
                snapshot.inForce().value(),
                snapshot.status().name(),
                snapshot.activities().stream().map(ExpeditionMapping::execution).toList(),
                snapshot.incidents().stream().map(ExpeditionMapping::incident).toList(),
                snapshot.observations().stream().map(ExpeditionMapping::observation).toList()
        );
    }

    public static ValidationResponse validation(ValidationResult result) {
        return new ValidationResponse(
                result.expeditionId().value(),
                result.version(),
                result.issues().stream().map(ExpeditionMapping::issue).toList()
        );
    }

    public static ProposalResponse proposal(ProposalSnapshot snapshot) {
        return new ProposalResponse(
                snapshot.id().value(),
                snapshot.originalId().value(),
                incident(snapshot.incident()),
                snapshot.decision().name(),
                snapshot.decidedBy().map(PersonId::value).orElse(null),
                snapshot.decidedAt().orElse(null),
                expedition(snapshot.suggested())
        );
    }

    private static ItineraryItem item(ItineraryNode node, Supplier<ActivityId> ids) {
        return switch (node) {
            case ActivityNode activity -> activity(activity.activity(), ids.get());
            case BlockNode block -> block(block, ids);
        };
    }

    private static ItineraryItemResponse item(ItineraryItem item) {
        return switch (item) {
            case Activity activity -> activity(activity);
            case ActivityBlock block -> new BlockResponse(
                    block.arrangement().name(),
                    block.parts().stream().map(ExpeditionMapping::item).toList()
            );
        };
    }

    public static ActivityResponse activity(Activity activity) {
        return new ActivityResponse(
                activity.id().value(),
                activity.name(),
                activity.estimatedDuration().toString(),
                activity.risk().name(),
                amounts(activity.estimatedConsumption()),
                activity.zone().name(),
                Periods.toResponse(activity.window()),
                activity.predecessors().stream().map(ActivityId::value).sorted().toList(),
                requirements(activity.requirements())
        );
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

    private static AssignmentResponse assignment(Assignment assignment) {
        return switch (assignment) {
            case PersonAssignment person -> new PersonAssignmentResponse(person.activityId().value(), person.personId().value());
            case VehicleAssignment vehicle -> new VehicleAssignmentResponse(vehicle.activityId().value(), vehicle.vehicleId().value());
            case InstrumentAssignment instrument -> new InstrumentAssignmentResponse(instrument.activityId().value(), instrument.instrumentId().value());
            case ConsumableAssignment consumable -> new ConsumableAssignmentResponse(
                    consumable.activityId().value(),
                    consumable.consumableId().value(),
                    consumable.quantity().amount()
            );
            default -> throw new IllegalStateException("unsupported assignment");
        };
    }

    private static CharterResponse charter(ExpeditionCharter charter) {
        return new CharterResponse(
                charter.objectives().stream().map(Objective::text).toList(),
                Periods.toResponse(charter.period()),
                charter.zones().stream().map(WorkZone::name).toList(),
                charter.responsibles().stream().map(PersonId::value).toList(),
                charter.restrictions().stream().map(Restriction::text).toList()
        );
    }

    private static AcceptedWarningResponse warning(AcceptedWarning warning) {
        return new AcceptedWarningResponse(
                warning.issue().severity().name(),
                warning.issue().code(),
                warning.issue().message(),
                warning.justification(),
                warning.acceptedBy().value()
        );
    }

    private static IncidentResponse incident(Incident incident) {
        return new IncidentResponse(
                incident.description(),
                incident.at(),
                incident.activityId().map(ActivityId::value).orElse(null)
        );
    }

    private static ObservationResponse observation(Observation observation) {
        return new ObservationResponse(observation.text(), observation.at());
    }

    private static ActivityExecutionResponse execution(ActivityExecution execution) {
        return new ActivityExecutionResponse(
                execution.activityId().value(),
                execution.startedAt(),
                execution.finishedAt().orElse(null),
                execution.result().orElse(null)
        );
    }

    private static ActivityResultResponse result(ActivityResult result) {
        return new ActivityResultResponse(result.activityId().value(), result.result());
    }

    private static IssueResponse issue(ValidationIssue issue) {
        return new IssueResponse(issue.severity().name(), issue.code(), issue.message());
    }

    private static Map<ConsumableId, Stock> consumption(Map<UUID, Integer> amounts) {
        Map<ConsumableId, Stock> consumption = new LinkedHashMap<>();
        amounts.forEach((id, amount) -> consumption.put(new ConsumableId(id), new Stock(amount)));
        return consumption;
    }

    private static Map<String, Integer> amounts(Map<ConsumableId, Stock> consumption) {
        Map<String, Integer> amounts = new LinkedHashMap<>();
        consumption.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(ConsumableId::value)))
                .forEach(entry -> amounts.put(entry.getKey().value().toString(), entry.getValue().amount()));
        return amounts;
    }

    private static Set<ActivityId> predecessors(Set<UUID> ids) {
        return ids.stream().map(ActivityId::new).collect(Collectors.toSet());
    }
}
