package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
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

        assertThrows(UnknownResource.class, () -> personnel.registerPerson("Ada", List.of(unknown), Availability.always()));
    }

    @Test
    void rejectsChangingAnUnknownResource() {
        VehicleId unknown = new VehicleId(UUID.randomUUID());

        assertThrows(UnknownResource.class, () -> equipment.changeAvailability(unknown, Availability.always()));
    }

    @Test
    void pagesThroughPeopleInTheOrderTheyWereRegistered() {
        PersonId ada = personnel.registerPerson("Ada", List.of(), Availability.always());
        PersonId bob = personnel.registerPerson("Bob", List.of(), Availability.always());
        PersonId eve = personnel.registerPerson("Eve", List.of(), Availability.always());

        Page<Person> first = consultPersonnel.people(new PageRequest(0, 2));
        Page<Person> second = consultPersonnel.people(new PageRequest(1, 2));

        assertAll(
                () -> assertEquals(List.of(ada, bob), first.items().stream().map(Person::id).toList()),
                () -> assertEquals(List.of(eve), second.items().stream().map(Person::id).toList()),
                () -> assertEquals(3, second.totalItems()),
                () -> assertEquals(2, second.totalPages())
        );
    }

    @Test
    void consultsEachRegisteredResourceById() {
        CertificationId sampling = personnel.registerCertification("Sampling");
        PersonId ada = personnel.registerPerson("Ada", List.of(sampling), Availability.always());
        VehicleId boat = equipment.registerVehicle(new Passengers(4), Availability.always());
        InstrumentId probe = equipment.registerInstrument(new InstrumentKind("probe"), Availability.always());
        ConsumableId vials = equipment.registerConsumable("vials", new Stock(20));
        PermitId permit = permitting.registerPermit(PermitKind.ZONE, DELTA, PERIOD);

        assertAll(
                () -> assertEquals("Sampling", consultPersonnel.certification(sampling).name()),
                () -> assertEquals(List.of(sampling), consultPersonnel.person(ada).certifications().stream().map(Certification::id).toList()),
                () -> assertEquals(new Passengers(4), consultEquipment.vehicle(boat).capacity()),
                () -> assertEquals(new InstrumentKind("probe"), consultEquipment.instrument(probe).kind()),
                () -> assertEquals(new Stock(20), consultEquipment.consumable(vials).stock()),
                () -> assertEquals(new WorkZone("Delta"), consultPermits.permit(permit).zone())
        );
    }

    @Test
    void listsEveryKindOfCatalogResource() {
        personnel.registerCertification("Sampling");
        equipment.registerVehicle(new Passengers(4), Availability.always());
        equipment.registerInstrument(new InstrumentKind("probe"), Availability.always());
        equipment.registerConsumable("vials", new Stock(20));
        permitting.registerPermit(PermitKind.ZONE, DELTA, PERIOD);

        assertAll(
                () -> assertEquals(1, consultPersonnel.certifications(FIRST_PAGE).totalItems()),
                () -> assertEquals(1, consultEquipment.vehicles(FIRST_PAGE).totalItems()),
                () -> assertEquals(1, consultEquipment.instruments(FIRST_PAGE).totalItems()),
                () -> assertEquals(1, consultEquipment.consumables(FIRST_PAGE).totalItems()),
                () -> assertEquals(1, consultPermits.permits(FIRST_PAGE).totalItems())
        );
    }

    @Test
    void rejectsConsultingAnUnknownResource() {
        assertAll(
                () -> assertThrows(UnknownResource.class, () -> consultPersonnel.person(new PersonId(UUID.randomUUID()))),
                () -> assertThrows(UnknownResource.class, () -> consultEquipment.vehicle(new VehicleId(UUID.randomUUID()))),
                () -> assertThrows(UnknownResource.class, () -> consultPermits.permit(new PermitId(UUID.randomUUID())))
        );
    }
}
