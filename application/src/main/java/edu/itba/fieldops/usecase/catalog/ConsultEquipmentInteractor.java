package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.Objects;

public final class ConsultEquipmentInteractor implements ConsultEquipment {
    private final VehicleRegistry vehicles;
    private final InstrumentRegistry instruments;
    private final ConsumableRegistry consumables;

    public ConsultEquipmentInteractor(VehicleRegistry vehicles, InstrumentRegistry instruments, ConsumableRegistry consumables) {
        this.vehicles = Objects.requireNonNull(vehicles, "vehicles");
        this.instruments = Objects.requireNonNull(instruments, "instruments");
        this.consumables = Objects.requireNonNull(consumables, "consumables");
    }

    @Override
    public Page<Vehicle> vehicles(PageRequest request) {
        return vehicles.vehicles(request);
    }

    @Override
    public Vehicle vehicle(VehicleId vehicleId) {
        return vehicles.require(vehicleId);
    }

    @Override
    public Page<Instrument> instruments(PageRequest request) {
        return instruments.instruments(request);
    }

    @Override
    public Instrument instrument(InstrumentId instrumentId) {
        return instruments.require(instrumentId);
    }

    @Override
    public Page<Consumable> consumables(PageRequest request) {
        return consumables.consumables(request);
    }

    @Override
    public Consumable consumable(ConsumableId consumableId) {
        return consumables.require(consumableId);
    }
}
