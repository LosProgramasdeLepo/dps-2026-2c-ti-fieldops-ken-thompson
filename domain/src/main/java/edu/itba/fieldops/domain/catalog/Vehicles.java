package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.VehicleId;

import java.util.List;
import java.util.Optional;

public interface Vehicles {
    Optional<Vehicle> vehicle(VehicleId id);

    List<Vehicle> vehicles();
}
