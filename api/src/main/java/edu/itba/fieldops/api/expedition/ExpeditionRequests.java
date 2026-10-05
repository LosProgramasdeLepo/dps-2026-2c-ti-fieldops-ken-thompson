package edu.itba.fieldops.api.expedition;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.itba.fieldops.api.json.PeriodRequest;
import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.shared.RiskLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ExpeditionRequests {
    private ExpeditionRequests() {
    }

    public record CharterRequest(
            @NotNull @Size(min = 1, max = 20) List<@NotBlank @Size(max = 500) String> objectives,
            @NotNull @Valid PeriodRequest period,
            @NotNull @Size(min = 1, max = 20) List<@NotBlank @Size(max = 200) String> zones,
            @NotNull @Size(min = 1, max = 20) List<@NotNull UUID> responsibles,
            @NotNull @Size(max = 20) List<@NotBlank @Size(max = 500) String> restrictions
    ) {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SamplingActivity.class, name = "SAMPLING"),
            @JsonSubTypes.Type(value = MeasurementActivity.class, name = "MEASUREMENT"),
            @JsonSubTypes.Type(value = TransitActivity.class, name = "TRANSIT"),
            @JsonSubTypes.Type(value = NightActivity.class, name = "NIGHT"),
            @JsonSubTypes.Type(value = DiveActivity.class, name = "DIVE"),
            @JsonSubTypes.Type(value = CampActivity.class, name = "CAMP")
    })
    public sealed interface ActivityRequest permits SamplingActivity, MeasurementActivity, TransitActivity, NightActivity, DiveActivity, CampActivity {
        String name();

        Duration estimatedDuration();

        RiskLevel risk();

        Map<UUID, Integer> consumption();

        String zone();

        PeriodRequest window();

        Set<UUID> predecessors();
    }

    public record SamplingActivity(
            @NotNull UUID certification,
            @NotBlank @Size(max = 200) String name,
            @NotNull Duration estimatedDuration,
            @NotNull RiskLevel risk,
            @NotNull @Size(max = 20) Map<@NotNull UUID, @NotNull @Min(0) Integer> consumption,
            @NotBlank @Size(max = 200) String zone,
            @NotNull @Valid PeriodRequest window,
            @NotNull @Size(max = 20) Set<@NotNull UUID> predecessors
    ) implements ActivityRequest {
    }

    public record MeasurementActivity(
            @NotNull UUID certification,
            @NotBlank @Size(max = 100) String instrument,
            @NotBlank @Size(max = 200) String name,
            @NotNull Duration estimatedDuration,
            @NotNull RiskLevel risk,
            @NotNull @Size(max = 20) Map<@NotNull UUID, @NotNull @Min(0) Integer> consumption,
            @NotBlank @Size(max = 200) String zone,
            @NotNull @Valid PeriodRequest window,
            @NotNull @Size(max = 20) Set<@NotNull UUID> predecessors
    ) implements ActivityRequest {
    }

    public record TransitActivity(
            @NotBlank @Size(max = 200) String name,
            @NotNull Duration estimatedDuration,
            @NotNull RiskLevel risk,
            @NotNull @Size(max = 20) Map<@NotNull UUID, @NotNull @Min(0) Integer> consumption,
            @NotBlank @Size(max = 200) String zone,
            @NotNull @Valid PeriodRequest window,
            @NotNull @Size(max = 20) Set<@NotNull UUID> predecessors
    ) implements ActivityRequest {
    }

    public record NightActivity(
            @NotNull UUID certification,
            @NotBlank @Size(max = 200) String name,
            @NotNull Duration estimatedDuration,
            @NotNull RiskLevel risk,
            @NotNull @Size(max = 20) Map<@NotNull UUID, @NotNull @Min(0) Integer> consumption,
            @NotBlank @Size(max = 200) String zone,
            @NotNull @Valid PeriodRequest window,
            @NotNull @Size(max = 20) Set<@NotNull UUID> predecessors
    ) implements ActivityRequest {
    }

    public record DiveActivity(
            @NotNull UUID certification,
            @NotBlank @Size(max = 200) String name,
            @NotNull Duration estimatedDuration,
            @NotNull RiskLevel risk,
            @NotNull @Size(max = 20) Map<@NotNull UUID, @NotNull @Min(0) Integer> consumption,
            @NotBlank @Size(max = 200) String zone,
            @NotNull @Valid PeriodRequest window,
            @NotNull @Size(max = 20) Set<@NotNull UUID> predecessors
    ) implements ActivityRequest {
    }

    public record CampActivity(
            @NotBlank @Size(max = 200) String name,
            @NotNull Duration estimatedDuration,
            @NotNull RiskLevel risk,
            @NotNull @Size(max = 20) Map<@NotNull UUID, @NotNull @Min(0) Integer> consumption,
            @NotBlank @Size(max = 200) String zone,
            @NotNull @Valid PeriodRequest window,
            @NotNull @Size(max = 20) Set<@NotNull UUID> predecessors
    ) implements ActivityRequest {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "node")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ActivityNode.class, name = "ACTIVITY"),
            @JsonSubTypes.Type(value = BlockNode.class, name = "BLOCK")
    })
    public sealed interface ItineraryNode permits ActivityNode, BlockNode {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    public record ActivityNode(@NotNull @Valid ActivityRequest activity) implements ItineraryNode {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    public record BlockNode(
            @NotNull ActivityBlock.Arrangement arrangement,
            @NotNull @Size(min = 2, max = 20) List<@NotNull @Valid ItineraryNode> parts
    ) implements ItineraryNode {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = PersonAssignmentRequest.class, name = "PERSON"),
            @JsonSubTypes.Type(value = VehicleAssignmentRequest.class, name = "VEHICLE"),
            @JsonSubTypes.Type(value = InstrumentAssignmentRequest.class, name = "INSTRUMENT"),
            @JsonSubTypes.Type(value = ConsumableAssignmentRequest.class, name = "CONSUMABLE")
    })
    public sealed interface AssignmentRequest
            permits PersonAssignmentRequest, VehicleAssignmentRequest, InstrumentAssignmentRequest, ConsumableAssignmentRequest {
    }

    public record PersonAssignmentRequest(@NotNull UUID activityId, @NotNull UUID personId) implements AssignmentRequest {
    }

    public record VehicleAssignmentRequest(@NotNull UUID activityId, @NotNull UUID vehicleId) implements AssignmentRequest {
    }

    public record InstrumentAssignmentRequest(@NotNull UUID activityId, @NotNull UUID instrumentId) implements AssignmentRequest {
    }

    public record ConsumableAssignmentRequest(
            @NotNull UUID activityId,
            @NotNull UUID consumableId,
            @NotNull @Min(1) Integer quantity
    ) implements AssignmentRequest {
    }

    public record IssueRequest(
            @NotNull IssueSeverity severity,
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 500) String message
    ) {
    }

    public record AcceptWarningRequest(
            @NotNull @Valid IssueRequest issue,
            @NotBlank @Size(max = 2000) String justification,
            @NotNull UUID acceptedBy
    ) {
    }

    public record DelayRequest(@NotNull Duration delay) {
    }

    public record StartActivityRequest(@NotNull UUID activityId) {
    }

    public record FinishActivityRequest(@NotBlank @Size(max = 2000) String result) {
    }

    public record ObservationRequest(@NotBlank @Size(max = 2000) String text) {
    }

    public record IncidentRequest(
            @NotBlank @Size(max = 2000) String description,
            UUID activityId
    ) {
    }

    public record DecisionRequest(@NotNull UUID responsible) {
    }
}
