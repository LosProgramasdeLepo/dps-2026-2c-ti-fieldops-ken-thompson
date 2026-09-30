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

    void save(Person person);

    void save(Vehicle vehicle);

    void save(Instrument instrument);

    void save(Consumable consumable);

    void save(Permit permit);
}
