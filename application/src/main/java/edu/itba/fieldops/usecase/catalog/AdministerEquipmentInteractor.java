package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.Stock;

import java.util.Objects;

public final class AdministerEquipmentInteractor implements AdministerEquipment {
    private final VehicleRegistry vehicles;
    private final InstrumentRegistry instruments;
    private final ConsumableRegistry consumables;

    public AdministerEquipmentInteractor(VehicleRegistry vehicles, InstrumentRegistry instruments, ConsumableRegistry consumables) {
        this.vehicles = Objects.requireNonNull(vehicles, "vehicles");
        this.instruments = Objects.requireNonNull(instruments, "instruments");
        this.consumables = Objects.requireNonNull(consumables, "consumables");
    }

    @Override
    public VehicleId registerVehicle(Passengers capacity, Availability availability) {
        VehicleId id = vehicles.nextVehicleId();
        vehicles.save(new Vehicle(id, capacity, availability));
        return id;
    }

    @Override
    public InstrumentId registerInstrument(InstrumentKind kind, Availability availability) {
        InstrumentId id = instruments.nextInstrumentId();
        instruments.save(new Instrument(id, kind, availability));
        return id;
    }

    @Override
    public ConsumableId registerConsumable(String name, Stock stock) {
        ConsumableId id = consumables.nextConsumableId();
        consumables.save(new Consumable(id, name, stock));
        return id;
    }

    @Override
    public void changeAvailability(VehicleId vehicleId, Availability availability) {
        vehicles.save(vehicles.require(vehicleId).withAvailability(availability));
    }

    @Override
    public void changeAvailability(InstrumentId instrumentId, Availability availability) {
        instruments.save(instruments.require(instrumentId).withAvailability(availability));
    }

    @Override
    public void changeStock(ConsumableId consumableId, Stock stock) {
        consumables.save(consumables.require(consumableId).withStock(stock));
    }
}
