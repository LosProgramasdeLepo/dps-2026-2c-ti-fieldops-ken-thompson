package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.catalog.VehicleRegistry;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.SQL)
@Transactional(readOnly = true)
public class JpaVehicleRegistry implements VehicleRegistry {
    private final StoredCatalog<VehicleEntity, Vehicle> stored;

    public JpaVehicleRegistry(VehicleJpaRepository repository) {
        this.stored = new StoredCatalog<>(repository, VehicleEntity::toDomain);
    }

    @Override
    public VehicleId nextVehicleId() {
        return new VehicleId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");
        stored.save(new VehicleEntity(vehicle));
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
