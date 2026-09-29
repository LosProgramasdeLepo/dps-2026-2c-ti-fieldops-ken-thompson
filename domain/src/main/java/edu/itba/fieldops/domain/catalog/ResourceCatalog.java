package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ResourceCatalog implements Catalog {
    private final Map<PersonId, Person> people = new LinkedHashMap<>();
    private final Map<VehicleId, Vehicle> vehicles = new LinkedHashMap<>();
    private final Map<InstrumentId, Instrument> instruments = new LinkedHashMap<>();
    private final Map<ConsumableId, Consumable> consumables = new LinkedHashMap<>();
    private final Map<PermitId, Permit> permits = new LinkedHashMap<>();

    public void add(Person person) {
        put(people, person.id(), person, "person");
    }

    public void add(Vehicle vehicle) {
        put(vehicles, vehicle.id(), vehicle, "vehicle");
    }

    public void add(Instrument instrument) {
        put(instruments, instrument.id(), instrument, "instrument");
    }

    public void add(Consumable consumable) {
        put(consumables, consumable.id(), consumable, "consumable");
    }

    public void add(Permit permit) {
        put(permits, permit.id(), permit, "permit");
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

    private static <K, T> void put(Map<K, T> items, K id, T value, String type) {
        Objects.requireNonNull(value, type);
        if (items.putIfAbsent(id, value) != null) {
            throw new InvalidValue("duplicate " + type + ": " + id);
        }
    }

    private static <K, T> Optional<T> find(Map<K, T> items, K id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(items.get(id));
    }
}
