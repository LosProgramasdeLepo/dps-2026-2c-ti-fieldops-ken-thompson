package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.BookableId;
import edu.itba.fieldops.domain.identity.VehicleId;

import java.util.Objects;
import java.util.Optional;

public record VehicleAssignment(ActivityId activityId, VehicleId vehicleId) implements BookableAssignment {
    public VehicleAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(vehicleId, "vehicle id");
    }

    @Override
    public BookableId resourceId() {
        return vehicleId;
    }

    @Override
    public Optional<Vehicle> resourceIn(BookableResources resources) {
        return resources.vehicles().vehicle(vehicleId);
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
