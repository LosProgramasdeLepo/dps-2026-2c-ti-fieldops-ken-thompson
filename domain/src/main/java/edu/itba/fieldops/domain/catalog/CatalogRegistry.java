package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;

public interface CatalogRegistry {
    PersonId nextPersonId();

    VehicleId nextVehicleId();

    InstrumentId nextInstrumentId();

    ConsumableId nextConsumableId();

    PermitId nextPermitId();

    void add(Person person);

    void add(Vehicle vehicle);

    void add(Instrument instrument);

    void add(Consumable consumable);

    void add(Permit permit);
}
