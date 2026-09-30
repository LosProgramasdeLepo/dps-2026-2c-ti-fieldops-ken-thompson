package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.catalog.usecase.AdministerEquipment;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.InvalidValue;
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
        Vehicle vehicle = vehicles.vehicle(vehicleId)
                .orElseThrow(() -> new InvalidValue("unknown vehicle: " + vehicleId));
        vehicles.save(vehicle.withAvailability(availability));
    }

    @Override
    public void changeAvailability(InstrumentId instrumentId, Availability availability) {
        Instrument instrument = instruments.instrument(instrumentId)
                .orElseThrow(() -> new InvalidValue("unknown instrument: " + instrumentId));
        instruments.save(instrument.withAvailability(availability));
    }

    @Override
    public void changeStock(ConsumableId consumableId, Stock stock) {
        Consumable consumable = consumables.consumable(consumableId)
                .orElseThrow(() -> new InvalidValue("unknown consumable: " + consumableId));
        consumables.save(consumable.withStock(stock));
    }
}
