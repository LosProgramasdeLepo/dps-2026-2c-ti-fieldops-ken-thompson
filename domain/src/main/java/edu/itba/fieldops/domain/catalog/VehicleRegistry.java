package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;

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
