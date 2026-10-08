package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.adapters.jpa.JpaRepositoryTest;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JpaRepositoryTest
class CatalogRepositoriesTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00.123456Z");
    private static final Availability WEEK = new Availability(List.of(
            new TimePeriod(DAY, DAY.plus(Duration.ofDays(2))),
            new TimePeriod(DAY.plus(Duration.ofDays(4)), DAY.plus(Duration.ofDays(7)))
    ));

    @Autowired
    private TestEntityManager entities;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private CertificationJpaRepository certificationRows;
    @Autowired
    private PersonJpaRepository personRows;
    @Autowired
    private VehicleJpaRepository vehicleRows;
    @Autowired
    private InstrumentJpaRepository instrumentRows;
    @Autowired
    private ConsumableJpaRepository consumableRows;
    @Autowired
    private PermitJpaRepository permitRows;

    private JpaCertificationRegistry certifications;
    private JpaPersonRegistry people;
    private JpaVehicleRegistry vehicles;
    private JpaInstrumentRegistry instruments;
    private JpaConsumableRegistry consumables;
    private JpaPermitRegistry permits;

    @BeforeEach
    void connect() {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        certifications = new JpaCertificationRegistry(certificationRows, transactions);
        people = new JpaPersonRegistry(personRows, certificationRows, transactions);
        vehicles = new JpaVehicleRegistry(vehicleRows, transactions);
        instruments = new JpaInstrumentRegistry(instrumentRows, transactions);
        consumables = new JpaConsumableRegistry(consumableRows, transactions);
        permits = new JpaPermitRegistry(permitRows, transactions);
    }

    @Test
    void aPersonKeepsTheCertificationsAndAvailabilityItWasSavedWith() {
        Certification diving = certification("Diving");
        Certification night = certification("Night operation");
        Person ada = new Person(people.nextPersonId(), "Ada", List.of(diving, night), WEEK);

        people.save(ada);
        reload();

        Person stored = people.require(ada.id());
        assertAll(
                () -> assertEquals("Ada", stored.name()),
                () -> assertEquals(List.of(diving, night), stored.certifications()),
                () -> assertEquals(WEEK, stored.availability())
        );
    }

    @Test
    void savingAPersonAgainReplacesItsCertificationsInPlace() {
        Certification diving = certification("Diving");
        Person ada = new Person(people.nextPersonId(), "Ada", List.of(), Availability.always());
        people.save(ada);
        reload();

        people.save(people.require(ada.id()).certified(diving).withAvailability(WEEK));
        reload();

        assertAll(
                () -> assertEquals(List.of(diving), people.require(ada.id()).certifications()),
                () -> assertEquals(WEEK, people.require(ada.id()).availability()),
                () -> assertEquals(1, people.people().size())
        );
    }

    @Test
    void equipmentAndPermitsKeepTheDataTheyWereSavedWith() {
        Vehicle truck = new Vehicle(vehicles.nextVehicleId(), new Passengers(6), WEEK);
        Instrument lamp = new Instrument(instruments.nextInstrumentId(), InstrumentKind.LIGHTING, WEEK);
        Consumable fuel = new Consumable(consumables.nextConsumableId(), "Fuel", new Stock(40));
        Permit nightDelta = new Permit(
                permits.nextPermitId(),
                new WorkZone("Delta"),
                new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                PermitKind.NIGHT
        );

        vehicles.save(truck);
        instruments.save(lamp);
        consumables.save(fuel);
        permits.save(nightDelta);
        reload();

        Vehicle storedTruck = vehicles.require(truck.id());
        Instrument storedLamp = instruments.require(lamp.id());
        Consumable storedFuel = consumables.require(fuel.id());
        assertAll(
                () -> assertEquals(new Passengers(6), storedTruck.capacity()),
                () -> assertEquals(WEEK, storedTruck.availability()),
                () -> assertEquals(InstrumentKind.LIGHTING, storedLamp.kind()),
                () -> assertEquals(WEEK, storedLamp.availability()),
                () -> assertEquals("Fuel", storedFuel.name()),
                () -> assertEquals(new Stock(40), storedFuel.stock()),
                () -> assertEquals(nightDelta, permits.require(nightDelta.id()))
        );
    }

    @Test
    void listsAndPagesInTheOrderTheyWereRegistered() {
        List<Vehicle> registered = List.of(vehicle(2), vehicle(4), vehicle(8));
        registered.forEach(vehicles::save);
        reload();

        Page<Vehicle> first = vehicles.vehicles(new PageRequest(0, 2));
        Page<Vehicle> second = vehicles.vehicles(new PageRequest(1, 2));

        assertAll(
                () -> assertEquals(ids(registered), ids(vehicles.vehicles())),
                () -> assertEquals(ids(registered.subList(0, 2)), ids(first.items())),
                () -> assertEquals(ids(registered.subList(2, 3)), ids(second.items())),
                () -> assertEquals(3, first.totalItems()),
                () -> assertEquals(2, first.totalPages())
        );
    }

    @Test
    void rejectsAPersonWithACertificationThatIsNotRegistered() {
        Certification unregistered = new Certification(new CertificationId(UUID.randomUUID()), "Ghost");
        Person ada = new Person(people.nextPersonId(), "Ada", List.of(unregistered), WEEK);

        assertThrows(DataAccessException.class, () -> {
            people.save(ada);
            entities.flush();
        });
    }

    @Test
    void anUnknownIdIsNotFound() {
        CertificationId unknown = new CertificationId(UUID.randomUUID());

        assertAll(
                () -> assertEquals(Optional.empty(), certifications.certification(unknown)),
                () -> assertThrows(UnknownResource.class, () -> certifications.require(unknown))
        );
    }

    private Certification certification(String name) {
        Certification certification = new Certification(certifications.nextCertificationId(), name);
        certifications.save(certification);
        return certification;
    }

    private Vehicle vehicle(int capacity) {
        return new Vehicle(vehicles.nextVehicleId(), new Passengers(capacity), WEEK);
    }

    private void reload() {
        entities.flush();
        entities.clear();
    }

    private static List<UUID> ids(List<Vehicle> listed) {
        return listed.stream().map(vehicle -> vehicle.id().value()).toList();
    }
}
