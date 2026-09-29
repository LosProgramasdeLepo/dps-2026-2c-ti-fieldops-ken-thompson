package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Objects;
import java.util.Optional;

public record VehicleAssignment(ActivityId activityId, VehicleId vehicleId) implements Assignment {
    public VehicleAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(vehicleId, "vehicle id");
    }

    @Override
    public Optional<TemporalBooking> booking(TimePeriod window) {
        return Optional.of(new TemporalBooking.VehicleBooking(vehicleId, activityId, window));
    }

    @Override
    public Optional<String> unknownIn(Catalog catalog) {
        return catalog.vehicle(vehicleId).isEmpty() ? Optional.of("vehicle " + vehicleId) : Optional.empty();
    }
}
