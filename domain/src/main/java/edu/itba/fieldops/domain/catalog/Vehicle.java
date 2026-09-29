package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Objects;

public final class Vehicle {
    private final VehicleId id;
    private final Passengers capacity;
    private final Availability availability;

    public Vehicle(VehicleId id, Passengers capacity, Availability availability) {
        this.id = Objects.requireNonNull(id, "vehicle id");
        this.capacity = Objects.requireNonNull(capacity, "capacity");
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    public VehicleId id() {
        return id;
    }

    public Passengers capacity() {
        return capacity;
    }

    public boolean availableDuring(TimePeriod period) {
        return availability.covers(period);
    }
}
