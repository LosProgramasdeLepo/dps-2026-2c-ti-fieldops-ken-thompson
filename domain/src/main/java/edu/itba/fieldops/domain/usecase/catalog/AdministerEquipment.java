package edu.itba.fieldops.domain.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Capacity;
import edu.itba.fieldops.domain.catalog.ConsumableName;
import edu.itba.fieldops.domain.catalog.Stock;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InstrumentKind;

public interface AdministerEquipment {
    VehicleId registerVehicle(Capacity capacity, Availability availability);

    InstrumentId registerInstrument(InstrumentKind kind, Availability availability);

    ConsumableId registerConsumable(ConsumableName name, Stock stock);

    void changeAvailability(VehicleId vehicleId, Availability availability);

    void changeAvailability(InstrumentId instrumentId, Availability availability);

    void changeStock(ConsumableId consumableId, Stock stock);
}
