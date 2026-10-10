package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.usecase.catalog.VehicleRegistry;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JpaVehicleRegistry implements VehicleRegistry {
    private final StoredCatalog<VehicleEntity, Vehicle> stored;

    public JpaVehicleRegistry(VehicleJpaRepository repository, TransactionOperations transactions) {
        this.stored = new StoredCatalog<>(repository, VehicleEntity::toDomain, transactions);
    }

    @Override
    public VehicleId nextVehicleId() {
        return new VehicleId(UUID.randomUUID());
    }

    @Override
    public void save(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");
        stored.save(() -> new VehicleEntity(vehicle));
    }

    @Override
    public Optional<Vehicle> vehicle(VehicleId id) {
        return stored.find(Objects.requireNonNull(id, "vehicle id").value());
    }

    @Override
    public List<Vehicle> vehicles() {
        return stored.all();
    }

    @Override
    public Page<Vehicle> vehicles(PageRequest request) {
        return stored.page(request);
    }
}
