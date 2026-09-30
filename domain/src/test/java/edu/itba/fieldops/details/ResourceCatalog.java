package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.CatalogRegistry;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Certifications;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Consumables;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Instruments;
import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Permits;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.catalog.Vehicles;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ResourceCatalog implements People, Vehicles, Instruments, Consumables, Permits, Certifications, CatalogRegistry {
    private final Map<CertificationId, Certification> certifications = new LinkedHashMap<>();
    private final Map<PersonId, Person> people = new LinkedHashMap<>();
    private final Map<VehicleId, Vehicle> vehicles = new LinkedHashMap<>();
    private final Map<InstrumentId, Instrument> instruments = new LinkedHashMap<>();
    private final Map<ConsumableId, Consumable> consumables = new LinkedHashMap<>();
    private final Map<PermitId, Permit> permits = new LinkedHashMap<>();

    @Override
    public CertificationId nextCertificationId() {
        return new CertificationId(UUID.randomUUID());
    }

    @Override
    public PersonId nextPersonId() {
        return new PersonId(UUID.randomUUID());
    }

    @Override
    public VehicleId nextVehicleId() {
        return new VehicleId(UUID.randomUUID());
    }

    @Override
    public InstrumentId nextInstrumentId() {
        return new InstrumentId(UUID.randomUUID());
    }

    @Override
    public ConsumableId nextConsumableId() {
        return new ConsumableId(UUID.randomUUID());
    }

    @Override
    public PermitId nextPermitId() {
        return new PermitId(UUID.randomUUID());
    }

    @Override
    public void save(Certification certification) {
        put(certifications, certification.id(), certification, "certification");
    }

    @Override
    public void save(Person person) {
        put(people, person.id(), person, "person");
    }

    @Override
    public void save(Vehicle vehicle) {
        put(vehicles, vehicle.id(), vehicle, "vehicle");
    }

    @Override
    public void save(Instrument instrument) {
        put(instruments, instrument.id(), instrument, "instrument");
    }

    @Override
    public void save(Consumable consumable) {
        put(consumables, consumable.id(), consumable, "consumable");
    }

    @Override
    public void save(Permit permit) {
        put(permits, permit.id(), permit, "permit");
    }

    @Override
    public Optional<Certification> certification(CertificationId id) {
        return find(certifications, id);
    }

    @Override
    public Optional<Person> person(PersonId id) {
        return find(people, id);
    }

    @Override
    public Optional<Vehicle> vehicle(VehicleId id) {
        return find(vehicles, id);
    }

    @Override
    public Optional<Instrument> instrument(InstrumentId id) {
        return find(instruments, id);
    }

    @Override
    public Optional<Consumable> consumable(ConsumableId id) {
        return find(consumables, id);
    }

    @Override
    public Optional<Permit> permit(PermitId id) {
        return find(permits, id);
    }

    @Override
    public List<Person> people() {
        return List.copyOf(people.values());
    }

    @Override
    public List<Vehicle> vehicles() {
        return List.copyOf(vehicles.values());
    }

    @Override
    public List<Instrument> instruments() {
        return List.copyOf(instruments.values());
    }

    public BookableResources bookable() {
        return new BookableResources(this, this, this);
    }

    public Catalogs catalogs() {
        return new Catalogs(bookable(), this, this);
    }

    private static <K, T> void put(Map<K, T> items, K id, T value, String type) {
        items.put(id, Objects.requireNonNull(value, type));
    }

    private static <K, T> Optional<T> find(Map<K, T> items, K id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(items.get(id));
    }
}
