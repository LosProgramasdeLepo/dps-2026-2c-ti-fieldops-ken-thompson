package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.catalog.usecase.AdministerCatalog;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.List;
import java.util.Objects;

public final class AdministerCatalogInteractor implements AdministerCatalog {
    private final CatalogRegistry registry;
    private final Catalogs catalogs;

    public AdministerCatalogInteractor(CatalogRegistry registry, Catalogs catalogs) {
        this.registry = Objects.requireNonNull(registry, "catalog registry");
        this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
    }

    @Override
    public PersonId registerPerson(String name, List<Certification> certifications, Availability availability) {
        PersonId id = registry.nextPersonId();
        registry.save(new Person(id, name, certifications, availability));
        return id;
    }

    @Override
    public VehicleId registerVehicle(Passengers capacity, Availability availability) {
        VehicleId id = registry.nextVehicleId();
        registry.save(new Vehicle(id, capacity, availability));
        return id;
    }

    @Override
    public InstrumentId registerInstrument(InstrumentKind kind, Availability availability) {
        InstrumentId id = registry.nextInstrumentId();
        registry.save(new Instrument(id, kind, availability));
        return id;
    }

    @Override
    public ConsumableId registerConsumable(String name, Stock stock) {
        ConsumableId id = registry.nextConsumableId();
        registry.save(new Consumable(id, name, stock));
        return id;
    }

    @Override
    public PermitId registerPermit(WorkZone zone, TimePeriod validity) {
        PermitId id = registry.nextPermitId();
        registry.save(Permit.zone(id, zone, validity));
        return id;
    }

    @Override
    public PermitId registerNightPermit(WorkZone zone, TimePeriod validity) {
        PermitId id = registry.nextPermitId();
        registry.save(Permit.night(id, zone, validity));
        return id;
    }

    @Override
    public void changeAvailability(PersonId personId, Availability availability) {
        registry.save(requirePerson(personId).withAvailability(availability));
    }

    @Override
    public void changeAvailability(VehicleId vehicleId, Availability availability) {
        Vehicle vehicle = catalogs.vehicles().vehicle(vehicleId)
                .orElseThrow(() -> new InvalidValue("unknown vehicle: " + vehicleId));
        registry.save(vehicle.withAvailability(availability));
    }

    @Override
    public void changeAvailability(InstrumentId instrumentId, Availability availability) {
        Instrument instrument = catalogs.instruments().instrument(instrumentId)
                .orElseThrow(() -> new InvalidValue("unknown instrument: " + instrumentId));
        registry.save(instrument.withAvailability(availability));
    }

    @Override
    public void certify(PersonId personId, Certification certification) {
        registry.save(requirePerson(personId).certified(certification));
    }

    @Override
    public void changeStock(ConsumableId consumableId, Stock stock) {
        Consumable consumable = catalogs.consumables().consumable(consumableId)
                .orElseThrow(() -> new InvalidValue("unknown consumable: " + consumableId));
        registry.save(consumable.withStock(stock));
    }

    private Person requirePerson(PersonId personId) {
        return catalogs.people().person(personId)
                .orElseThrow(() -> new InvalidValue("unknown person: " + personId));
    }
}
