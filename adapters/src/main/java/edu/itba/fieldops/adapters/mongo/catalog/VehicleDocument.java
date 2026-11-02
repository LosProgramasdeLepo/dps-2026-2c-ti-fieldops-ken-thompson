package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.adapters.mongo.PeriodDocument;
import edu.itba.fieldops.domain.catalog.Capacity;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.VehicleId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document(collection = VehicleDocument.COLLECTION)
record VehicleDocument(@Id String id, int capacity, List<PeriodDocument> availability, long registrationOrder)
        implements RegisteredDocument {
    static final String COLLECTION = "vehicles";

    static VehicleDocument of(Vehicle vehicle, long registrationOrder) {
        return new VehicleDocument(
                vehicle.id().value().toString(),
                vehicle.capacity().seats(),
                PeriodDocument.of(vehicle.availability()),
                registrationOrder
        );
    }

    Vehicle toDomain() {
        return new Vehicle(new VehicleId(UUID.fromString(id)), new Capacity(capacity), PeriodDocument.availability(availability));
    }
}
