package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.adapters.jpa.StoredPeriod;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.Passengers;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class VehicleEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private int capacity;

    @ElementCollection
    @CollectionTable(name = "vehicle_availability", joinColumns = @JoinColumn(name = "vehicle_id"))
    @OrderColumn(name = "position")
    private List<StoredPeriod> availability = new ArrayList<>();

    @Column(name = "registration_order", insertable = false, updatable = false)
    private Long registrationOrder;

    protected VehicleEntity() {
    }

    VehicleEntity(Vehicle vehicle) {
        this.id = vehicle.id().value();
        this.capacity = vehicle.capacity().count();
        this.availability = new ArrayList<>(StoredPeriod.of(vehicle.availability()));
    }

    public Vehicle toDomain() {
        return new Vehicle(new VehicleId(id), new Passengers(capacity), StoredPeriod.availability(availability));
    }
}
