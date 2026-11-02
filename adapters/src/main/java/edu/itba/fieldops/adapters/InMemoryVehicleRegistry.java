package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.catalog.VehicleRegistry;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.MEMORY)
public class InMemoryVehicleRegistry implements VehicleRegistry {
    private final Map<VehicleId, Vehicle> vehicles = new LinkedHashMap<>();

    @Override
    public VehicleId nextVehicleId() {
        return new VehicleId(UUID.randomUUID());
    }

    @Override
    public synchronized void save(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");
        vehicles.put(vehicle.id(), vehicle);
    }

    @Override
    public synchronized Optional<Vehicle> vehicle(VehicleId id) {
        Objects.requireNonNull(id, "vehicle id");
        return Optional.ofNullable(vehicles.get(id));
    }

    @Override
    public synchronized List<Vehicle> vehicles() {
        return List.copyOf(vehicles.values());
    }

    @Override
    public synchronized Page<Vehicle> vehicles(PageRequest request) {
        return InMemoryPages.slice(vehicles.values(), request);
    }
}
