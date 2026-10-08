package edu.itba.fieldops.adapters.jpa.expedition;

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
import edu.itba.fieldops.domain.shared.Stock;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

record AssignmentRows(
        List<StoredBooking> people,
        List<StoredBooking> vehicles,
        List<StoredBooking> instruments,
        List<StoredConsumableAssignment> consumables
) {
    static AssignmentRows of(List<Assignment> assignments) {
        AssignmentRows rows = new AssignmentRows(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        assignments.forEach(rows::add);
        return rows;
    }

    List<Assignment> assignments() {
        return Stream.of(
                people.stream().<Assignment>map(row -> new PersonAssignment(
                        new ActivityId(row.activityId()), new PersonId(row.resourceId())
                )),
                vehicles.stream().<Assignment>map(row -> new VehicleAssignment(
                        new ActivityId(row.activityId()), new VehicleId(row.resourceId())
                )),
                instruments.stream().<Assignment>map(row -> new InstrumentAssignment(
                        new ActivityId(row.activityId()), new InstrumentId(row.resourceId())
                )),
                consumables.stream().<Assignment>map(row -> new ConsumableAssignment(
                        new ActivityId(row.activityId()), new ConsumableId(row.consumableId()), new Stock(row.quantity())
                ))
        ).flatMap(rows -> rows).toList();
    }

    private void add(Assignment assignment) {
        switch (assignment) {
            case PersonAssignment person -> people.add(
                    new StoredBooking(person.activityId().value(), person.personId().value())
            );
            case VehicleAssignment vehicle -> vehicles.add(
                    new StoredBooking(vehicle.activityId().value(), vehicle.vehicleId().value())
            );
            case InstrumentAssignment instrument -> instruments.add(
                    new StoredBooking(instrument.activityId().value(), instrument.instrumentId().value())
            );
            case ConsumableAssignment consumable -> consumables.add(new StoredConsumableAssignment(
                    consumable.activityId().value(), consumable.consumableId().value(), consumable.quantity().amount()
            ));
            default -> throw new IllegalArgumentException("unknown assignment: " + assignment);
        }
    }
}
