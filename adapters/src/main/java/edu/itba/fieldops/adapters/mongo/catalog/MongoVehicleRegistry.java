package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
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
@Profile(PersistenceProfiles.MONGO)
@Transactional(readOnly = true)
public class MongoVehicleRegistry implements VehicleRegistry {
    private final StoredCatalog<VehicleDocument, Vehicle> stored;

    MongoVehicleRegistry(VehicleMongoRepository repository, RegistrationSequence sequence) {
        this.stored = new StoredCatalog<>(repository, sequence, VehicleDocument.COLLECTION, VehicleDocument::toDomain);
    }

    @Override
    public VehicleId nextVehicleId() {
        return new VehicleId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");
        stored.save(vehicle.id().value(), order -> VehicleDocument.of(vehicle, order));
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
