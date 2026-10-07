package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.catalog.Vehicles;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.Objects;

public interface VehicleRegistry extends Vehicles {
    VehicleId nextVehicleId();

    void save(Vehicle vehicle);

    Page<Vehicle> vehicles(PageRequest request);

    default Vehicle require(VehicleId id) {
        return vehicle(Objects.requireNonNull(id, "vehicle id"))
                .orElseThrow(() -> new UnknownResource("vehicle", id.value().toString()));
    }
}
