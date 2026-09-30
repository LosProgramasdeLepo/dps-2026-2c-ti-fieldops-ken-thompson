package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.catalog.usecase.AdministerCatalog;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.List;
import java.util.Objects;

public final class AdministerCatalogInteractor implements AdministerCatalog {
    private final CatalogRegistry registry;
    private final Catalogs catalogs;
    private final Certifications certifications;

    public AdministerCatalogInteractor(CatalogRegistry registry, Catalogs catalogs, Certifications certifications) {
        this.registry = Objects.requireNonNull(registry, "catalog registry");
        this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
        this.certifications = Objects.requireNonNull(certifications, "certifications");
    }

    @Override
    public CertificationId registerCertification(String name) {
        CertificationId id = registry.nextCertificationId();
        registry.save(new Certification(id, name));
        return id;
    }

    @Override
    public PersonId registerPerson(String name, List<CertificationId> certificationIds, Availability availability) {
        PersonId id = registry.nextPersonId();
        registry.save(new Person(id, name, certificationIds.stream().map(this::requireCertification).toList(), availability));
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
    public PermitId registerPermit(PermitKind kind, WorkZone zone, TimePeriod validity) {
        PermitId id = registry.nextPermitId();
        registry.save(new Permit(id, zone, validity, kind));
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
    public void certify(PersonId personId, CertificationId certificationId) {
        registry.save(requirePerson(personId).certified(requireCertification(certificationId)));
    }

    @Override
    public void changeStock(ConsumableId consumableId, Stock stock) {
        Consumable consumable = catalogs.consumables().consumable(consumableId)
                .orElseThrow(() -> new InvalidValue("unknown consumable: " + consumableId));
        registry.save(consumable.withStock(stock));
    }

    private Certification requireCertification(CertificationId certificationId) {
        return certifications.certification(certificationId)
                .orElseThrow(() -> new InvalidValue("unknown certification: " + certificationId));
    }

    private Person requirePerson(PersonId personId) {
        return catalogs.people().person(personId)
                .orElseThrow(() -> new InvalidValue("unknown person: " + personId));
    }
}
