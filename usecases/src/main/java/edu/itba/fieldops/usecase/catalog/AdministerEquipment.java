package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.Stock;

public interface AdministerEquipment {
    VehicleId registerVehicle(Passengers capacity, Availability availability);

    InstrumentId registerInstrument(InstrumentKind kind, Availability availability);

    ConsumableId registerConsumable(String name, Stock stock);

    void changeAvailability(VehicleId vehicleId, Availability availability);

    void changeAvailability(InstrumentId instrumentId, Availability availability);

    void changeStock(ConsumableId consumableId, Stock stock);
}
