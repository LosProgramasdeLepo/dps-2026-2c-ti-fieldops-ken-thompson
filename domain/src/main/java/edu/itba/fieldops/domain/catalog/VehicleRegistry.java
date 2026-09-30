package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.VehicleId;

public interface VehicleRegistry extends Vehicles {
    VehicleId nextVehicleId();

    void save(Vehicle vehicle);
}
