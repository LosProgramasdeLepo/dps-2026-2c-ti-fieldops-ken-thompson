package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Objects;
import java.util.Optional;

public record VehicleAssignment(ActivityId activityId, VehicleId vehicleId) implements BookableAssignment {
    public VehicleAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(vehicleId, "vehicle id");
    }

    @Override
    public TemporalBooking booking(TimePeriod window) {
        return new TemporalBooking.VehicleBooking(vehicleId, activityId, window);
    }

    @Override
    public Optional<String> unknownIn(Catalogs catalogs) {
        return catalogs.vehicles().vehicle(vehicleId).isEmpty() ? Optional.of("vehicle " + vehicleId) : Optional.empty();
    }

    @Override
    public void fileInto(Assignments assignments) {
        assignments.file(this);
    }

    @Override
    public void withdrawFrom(Assignments assignments) {
        assignments.withdraw(this);
    }
}
