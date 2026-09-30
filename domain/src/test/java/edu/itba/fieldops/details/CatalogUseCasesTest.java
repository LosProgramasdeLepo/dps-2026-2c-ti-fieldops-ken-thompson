package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.catalog.Availability;
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
        CertificationId sampling = personnel.registerCertification("Sampling");

        PersonId personId = personnel.registerPerson("Ada", List.of(sampling), Availability.always());
        VehicleId vehicleId = equipment.registerVehicle(new Passengers(4), Availability.always());
        InstrumentId instrumentId = equipment.registerInstrument(new InstrumentKind("probe"), Availability.always());
        ConsumableId vials = equipment.registerConsumable("vials", new Stock(20));
        PermitId permitId = permitting.registerPermit(PermitKind.ZONE, DELTA, PERIOD);

        assertAll(
                () -> assertTrue(catalog.person(personId).orElseThrow().holds(sampling)),
                () -> assertTrue(catalog.vehicle(vehicleId).isPresent()),
                () -> assertTrue(catalog.instrument(instrumentId).isPresent()),
                () -> assertTrue(catalog.consumable(vials).isPresent()),
                () -> assertTrue(catalog.permit(permitId).isPresent())
        );
    }

    @Test
    void changesAvailabilityCertifiesAndChangesStock() {
        PersonId ada = personnel.registerPerson("Ada", List.of(), Availability.always());
        VehicleId boat = equipment.registerVehicle(new Passengers(4), Availability.always());
        InstrumentId lamp = equipment.registerInstrument(InstrumentKind.LIGHTING, Availability.always());
        ConsumableId vials = equipment.registerConsumable("vials", new Stock(20));
        CertificationId diving = personnel.registerCertification("Diving");
        Availability afternoon = new Availability(List.of(hours(6, 10)));
        TimePeriod morning = hours(0, 4);

        personnel.changeAvailability(ada, afternoon);
        equipment.changeAvailability(boat, afternoon);
        equipment.changeAvailability(lamp, afternoon);
        personnel.certify(ada, diving);
        equipment.changeStock(vials, new Stock(5));

        assertAll(
                () -> assertFalse(catalog.person(ada).orElseThrow().availableDuring(morning)),
                () -> assertTrue(catalog.person(ada).orElseThrow().holds(diving)),
                () -> assertFalse(catalog.vehicle(boat).orElseThrow().availableDuring(morning)),
                () -> assertFalse(catalog.instrument(lamp).orElseThrow().availableDuring(morning)),
                () -> assertEquals(new Stock(5), catalog.consumable(vials).orElseThrow().stock())
        );
    }

    @Test
    void rejectsACertificationThePersonAlreadyHolds() {
        CertificationId sampling = personnel.registerCertification("Sampling");
        PersonId ada = personnel.registerPerson("Ada", List.of(sampling), Availability.always());

        assertThrows(InvalidValue.class, () -> personnel.certify(ada, sampling));
    }

    @Test
    void rejectsAnUnknownCertification() {
        CertificationId unknown = new CertificationId(UUID.randomUUID());

        assertThrows(InvalidValue.class, () -> personnel.registerPerson("Ada", List.of(unknown), Availability.always()));
    }

    @Test
    void rejectsChangingAnUnknownResource() {
        VehicleId unknown = new VehicleId(UUID.randomUUID());

        assertThrows(InvalidValue.class, () -> equipment.changeAvailability(unknown, Availability.always()));
    }
}
