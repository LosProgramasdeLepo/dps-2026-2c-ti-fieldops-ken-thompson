package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.Vehicle;
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
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceCatalogTest {
    @Test
    void storesAndFindsEachResourceType() {
        ResourceCatalog catalog = new ResourceCatalog();
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(), Availability.always());
        Vehicle vehicle = new Vehicle(new VehicleId(UUID.randomUUID()), new Passengers(4), Availability.always());
        Instrument instrument = new Instrument(new InstrumentId(UUID.randomUUID()), new InstrumentKind("pH meter"), Availability.always());
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(20));
        Permit permit = Permit.zone(
                new PermitId(UUID.randomUUID()),
                new WorkZone("Delta"),
                new TimePeriod(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-08T00:00:00Z"))
        );

        catalog.save(person);
        catalog.save(vehicle);
        catalog.save(instrument);
        catalog.save(vials);
        catalog.save(permit);

        assertAll(
                () -> assertEquals(person, catalog.person(person.id()).orElseThrow()),
                () -> assertEquals(vehicle, catalog.vehicle(vehicle.id()).orElseThrow()),
                () -> assertEquals(instrument, catalog.instrument(instrument.id()).orElseThrow()),
                () -> assertEquals(vials, catalog.consumable(vials.id()).orElseThrow()),
                () -> assertEquals(permit, catalog.permit(permit.id()).orElseThrow()),
                () -> assertTrue(catalog.person(new PersonId(UUID.randomUUID())).isEmpty()),
                () -> assertEquals(List.of(person), catalog.people()),
                () -> assertEquals(List.of(vehicle), catalog.vehicles()),
                () -> assertEquals(List.of(instrument), catalog.instruments())
        );
    }

    @Test
    void savingTheSameIdReplacesTheResource() {
        ResourceCatalog catalog = new ResourceCatalog();
        PersonId id = new PersonId(UUID.randomUUID());
        Person before = new Person(id, "Ada", List.of(), Availability.always());
        Person after = new Person(id, "Ada Lovelace", List.of(), Availability.always());
        catalog.save(before);

        catalog.save(after);

        assertEquals(List.of(after), catalog.people());
    }
}
