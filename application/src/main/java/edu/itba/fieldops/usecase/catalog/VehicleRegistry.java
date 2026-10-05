package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.catalog.Vehicles;
import edu.itba.fieldops.domain.identity.VehicleId;

public interface VehicleRegistry extends Vehicles {
    VehicleId nextVehicleId();

    void save(Vehicle vehicle);
}
