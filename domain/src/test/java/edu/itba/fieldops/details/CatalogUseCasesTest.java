package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.identity.CertificationId;
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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogUseCasesTest extends UseCaseFixture {
    @Test
    void registersEachCatalogResourceAndKeepsCertificationOnThePerson() {
        Certification sampling = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");

        PersonId personId = registry.registerPerson("Ada", List.of(sampling), Availability.always());
        VehicleId vehicleId = registry.registerVehicle(new Passengers(4), Availability.always());
        InstrumentId instrumentId = registry.registerInstrument(new InstrumentKind("probe"), Availability.always());
        ConsumableId vials = registry.registerConsumable("vials", new Stock(20));
        PermitId permitId = registry.registerPermit(DELTA, PERIOD);

        assertAll(
                () -> assertTrue(catalog.person(personId).orElseThrow().holds(sampling.id())),
                () -> assertTrue(catalog.vehicle(vehicleId).isPresent()),
                () -> assertTrue(catalog.instrument(instrumentId).isPresent()),
                () -> assertTrue(catalog.consumable(vials).isPresent()),
                () -> assertTrue(catalog.permit(permitId).isPresent())
        );
    }

    @Test
    void changesAvailabilityCertifiesAndChangesStock() {
        PersonId ada = registry.registerPerson("Ada", List.of(), Availability.always());
        VehicleId boat = registry.registerVehicle(new Passengers(4), Availability.always());
        InstrumentId lamp = registry.registerInstrument(InstrumentKind.LIGHTING, Availability.always());
        ConsumableId vials = registry.registerConsumable("vials", new Stock(20));
        Certification diving = new Certification(new CertificationId(UUID.randomUUID()), "Diving");
        Availability afternoon = new Availability(List.of(hours(6, 10)));
        TimePeriod morning = hours(0, 4);

        registry.changeAvailability(ada, afternoon);
        registry.changeAvailability(boat, afternoon);
        registry.changeAvailability(lamp, afternoon);
        registry.certify(ada, diving);
        registry.changeStock(vials, new Stock(5));

        assertAll(
                () -> assertFalse(catalog.person(ada).orElseThrow().availableDuring(morning)),
                () -> assertTrue(catalog.person(ada).orElseThrow().holds(diving.id())),
                () -> assertFalse(catalog.vehicle(boat).orElseThrow().availableDuring(morning)),
                () -> assertFalse(catalog.instrument(lamp).orElseThrow().availableDuring(morning)),
                () -> assertEquals(new Stock(5), catalog.consumable(vials).orElseThrow().stock())
        );
    }

    @Test
    void rejectsACertificationThePersonAlreadyHolds() {
        Certification sampling = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        PersonId ada = registry.registerPerson("Ada", List.of(sampling), Availability.always());

        assertThrows(InvalidValue.class, () -> registry.certify(ada, sampling));
    }

    @Test
    void rejectsChangingAnUnknownResource() {
        VehicleId unknown = new VehicleId(UUID.randomUUID());

        assertThrows(InvalidValue.class, () -> registry.changeAvailability(unknown, Availability.always()));
    }
}
