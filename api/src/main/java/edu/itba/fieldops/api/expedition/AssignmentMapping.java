package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.AssignmentRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.ConsumableAssignmentRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.InstrumentAssignmentRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.PersonAssignmentRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.VehicleAssignmentRequest;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.AssignmentResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ConsumableAssignmentResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.InstrumentAssignmentResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.PersonAssignmentResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.VehicleAssignmentResponse;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.Consumption;

import java.util.List;

final class AssignmentMapping {
    private AssignmentMapping() {
    }

    static Assignment assignment(AssignmentRequest request) {
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
                    new Consumption(consumable.quantity())
            );
        };
    }

    static List<AssignmentResponse> assignments(List<Assignment> assignments) {
        return assignments.stream().map(AssignmentMapping::assignment).toList();
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
        };
    }
}
