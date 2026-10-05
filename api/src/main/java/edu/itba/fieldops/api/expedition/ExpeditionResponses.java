package edu.itba.fieldops.api.expedition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.itba.fieldops.api.json.PeriodResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ExpeditionResponses {
    private ExpeditionResponses() {
    }

    public record CharterResponse(
            List<String> objectives,
            PeriodResponse period,
            List<String> zones,
            List<UUID> responsibles,
            List<String> restrictions
    ) {
    }

    public record RequirementsResponse(
            List<UUID> certifications,
            List<UUID> heldByEveryone,
            List<String> instruments,
            List<String> specialPermits,
            int vehicles
    ) {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "node")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ActivityResponse.class, name = "ACTIVITY"),
            @JsonSubTypes.Type(value = BlockResponse.class, name = "BLOCK")
    })
    public sealed interface ItineraryItemResponse permits ActivityResponse, BlockResponse {
    }

    public record ActivityResponse(
            UUID id,
            String name,
            String estimatedDuration,
            String risk,
            Map<String, Integer> consumption,
            String zone,
            PeriodResponse window,
            List<UUID> predecessors,
            RequirementsResponse requirements
    ) implements ItineraryItemResponse {
    }

    public record BlockResponse(String arrangement, List<ItineraryItemResponse> parts) implements ItineraryItemResponse {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = PersonAssignmentResponse.class, name = "PERSON"),
            @JsonSubTypes.Type(value = VehicleAssignmentResponse.class, name = "VEHICLE"),
            @JsonSubTypes.Type(value = InstrumentAssignmentResponse.class, name = "INSTRUMENT"),
            @JsonSubTypes.Type(value = ConsumableAssignmentResponse.class, name = "CONSUMABLE")
    })
    public sealed interface AssignmentResponse
            permits PersonAssignmentResponse, VehicleAssignmentResponse, InstrumentAssignmentResponse, ConsumableAssignmentResponse {
    }

    public record PersonAssignmentResponse(UUID activityId, UUID personId) implements AssignmentResponse {
    }

    public record VehicleAssignmentResponse(UUID activityId, UUID vehicleId) implements AssignmentResponse {
    }

    public record InstrumentAssignmentResponse(UUID activityId, UUID instrumentId) implements AssignmentResponse {
    }

    public record ConsumableAssignmentResponse(UUID activityId, UUID consumableId, int quantity) implements AssignmentResponse {
    }

    public record AcceptedWarningResponse(
            String severity,
            String code,
            String message,
            String justification,
            UUID acceptedBy
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExpeditionResponse(
            UUID id,
            int version,
            UUID supersedes,
            String status,
            CharterResponse charter,
            List<ItineraryItemResponse> itinerary,
            List<AssignmentResponse> assignments,
            List<UUID> permits,
            List<AcceptedWarningResponse> acceptedWarnings
    ) {
    }

    public record EstimateResponse(String duration, String risk, Map<String, Integer> consumption) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IncidentResponse(String description, Instant at, UUID activityId) {
    }

    public record ObservationResponse(String text, Instant at) {
    }

    public record ActivityResultResponse(UUID activityId, String result) {
    }

    public record ReportResponse(
            String status,
            int plannedActivities,
            int startedActivities,
            int finishedActivities,
            String duration,
            String risk,
            Map<String, Integer> consumption,
            Map<String, Integer> estimatedConsumption,
            List<IncidentResponse> incidents,
            List<ObservationResponse> observations,
            List<ActivityResultResponse> activityResults
    ) {
    }

    public record ValidationResponse(UUID expeditionId, int version, List<IssueResponse> issues) {
    }

    public record IssueResponse(String severity, String code, String message) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProposalResponse(
            UUID id,
            UUID originalId,
            IncidentResponse incident,
            String decision,
            UUID decidedBy,
            Instant decidedAt,
            ExpeditionResponse suggested
    ) {
    }
}
