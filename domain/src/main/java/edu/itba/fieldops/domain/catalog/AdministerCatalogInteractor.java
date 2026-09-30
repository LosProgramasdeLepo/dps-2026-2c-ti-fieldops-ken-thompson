package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.catalog.usecase.AdministerCatalog;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.List;
import java.util.Objects;

public final class AdministerCatalogInteractor implements AdministerCatalog {
    private final CatalogRegistry registry;

    public AdministerCatalogInteractor(CatalogRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "catalog");
    }

    @Override
    public PersonId registerPerson(String name, List<Certification> certifications, Availability availability) {
        PersonId id = registry.nextPersonId();
        registry.add(new Person(id, name, certifications, availability));
        return id;
    }

    @Override
    public VehicleId registerVehicle(Passengers capacity, Availability availability) {
        VehicleId id = registry.nextVehicleId();
        registry.add(new Vehicle(id, capacity, availability));
        return id;
    }

    @Override
    public InstrumentId registerInstrument(InstrumentKind kind, Availability availability) {
        InstrumentId id = registry.nextInstrumentId();
        registry.add(new Instrument(id, kind, availability));
        return id;
    }

    @Override
    public ConsumableId registerConsumable(String name, Stock stock) {
        ConsumableId id = registry.nextConsumableId();
        registry.add(new Consumable(id, name, stock));
        return id;
    }

    @Override
    public PermitId registerPermit(WorkZone zone, TimePeriod validity) {
        PermitId id = registry.nextPermitId();
        registry.add(new Permit(id, zone, validity));
        return id;
    }
}
