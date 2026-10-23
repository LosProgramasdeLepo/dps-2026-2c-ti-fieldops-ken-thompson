package edu.itba.fieldops.domain.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;

public interface ConsultEquipment {
    Page<Vehicle> vehicles(PageRequest request);

    Vehicle vehicle(VehicleId vehicleId);

    Page<Instrument> instruments(PageRequest request);

    Instrument instrument(InstrumentId instrumentId);

    Page<Consumable> consumables(PageRequest request);

    Consumable consumable(ConsumableId consumableId);
}
