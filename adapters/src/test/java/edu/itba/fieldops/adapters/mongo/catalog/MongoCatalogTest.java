package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.adapters.mongo.MongoRepositoryTest;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Capacity;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.CertificationName;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.ConsumableName;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.catalog.Stock;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MongoRepositoryTest
class MongoCatalogTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final Availability WEEK = new Availability(List.of(
            new TimePeriod(DAY, DAY.plus(Duration.ofDays(2))),
            new TimePeriod(DAY.plus(Duration.ofDays(4)), DAY.plus(Duration.ofDays(7)))
    ));

    @Autowired
    private MongoTemplate mongo;
    @Autowired
    private CertificationMongoRepository certificationDocuments;
    @Autowired
    private PersonMongoRepository personDocuments;
    @Autowired
    private VehicleMongoRepository vehicleDocuments;
    @Autowired
    private InstrumentMongoRepository instrumentDocuments;
    @Autowired
    private ConsumableMongoRepository consumableDocuments;
    @Autowired
    private PermitMongoRepository permitDocuments;

    private MongoCertificationRegistry certifications;
    private MongoPersonRegistry people;
    private MongoVehicleRegistry vehicles;
    private MongoInstrumentRegistry instruments;
    private MongoConsumableRegistry consumables;
    private MongoPermitRegistry permits;

    @BeforeEach
    void connect() {
        mongo.getDb().drop();
        RegistrationSequence sequence = new RegistrationSequence(mongo.getMongoDatabaseFactory());
        certifications = new MongoCertificationRegistry(certificationDocuments, sequence);
        people = new MongoPersonRegistry(personDocuments, sequence);
        vehicles = new MongoVehicleRegistry(vehicleDocuments, sequence);
        instruments = new MongoInstrumentRegistry(instrumentDocuments, sequence);
        consumables = new MongoConsumableRegistry(consumableDocuments, sequence);
        permits = new MongoPermitRegistry(permitDocuments, sequence);
    }

    @Test
    void aPersonKeepsTheCertificationsAndAvailabilityItWasSavedWith() {
        Certification diving = certification("Diving");
        Person ada = person("Ada").certified(diving).withAvailability(WEEK);

        people.save(ada);

        Person stored = people.require(ada.id());
        assertAll(
                () -> assertEquals(ada.name(), stored.name()),
                () -> assertEquals(List.of(diving), stored.certifications()),
                () -> assertEquals(WEEK, stored.availability())
        );
    }

    @Test
    void equipmentAndPermitsKeepTheDataTheyWereSavedWith() {
        Vehicle truck = new Vehicle(vehicles.nextVehicleId(), new Capacity(4), WEEK);
        Instrument lamp = new Instrument(instruments.nextInstrumentId(), InstrumentKind.LIGHTING, WEEK);
        Consumable fuel = new Consumable(consumables.nextConsumableId(), new ConsumableName("Fuel"), new Stock(40));
        Permit night = new Permit(permits.nextPermitId(), new WorkZone("Delta"), WEEK.periods().getFirst(), PermitKind.NIGHT);
        vehicles.save(truck);
        instruments.save(lamp);
        consumables.save(fuel);
        permits.save(night);

        Vehicle storedTruck = vehicles.require(truck.id());
        Instrument storedLamp = instruments.require(lamp.id());
        Consumable storedFuel = consumables.require(fuel.id());
        assertAll(
                () -> assertEquals(new Capacity(4), storedTruck.capacity()),
                () -> assertEquals(WEEK, storedTruck.availability()),
                () -> assertEquals(InstrumentKind.LIGHTING, storedLamp.kind()),
                () -> assertEquals(WEEK, storedLamp.availability()),
                () -> assertEquals(fuel.name(), storedFuel.name()),
                () -> assertEquals(new Stock(40), storedFuel.stock()),
                () -> assertEquals(night, permits.require(night.id()))
        );
    }

    @Test
    void savingAgainKeepsTheOrderOfRegistrationThatListsAndPagesFollow() {
        Person ada = person("Ada");
        Person bob = person("Bob");
        Person eve = person("Eve");
        people.save(ada);
        people.save(bob);
        people.save(eve);

        people.save(ada.withAvailability(WEEK));

        Page<Person> first = people.people(new PageRequest(0, 2));
        assertAll(
                () -> assertEquals(List.of(ada.id(), bob.id(), eve.id()), people.people().stream().map(Person::id).toList()),
                () -> assertEquals(List.of(ada.id(), bob.id()), first.items().stream().map(Person::id).toList()),
                () -> assertEquals(3, first.totalItems()),
                () -> assertEquals(WEEK, people.require(ada.id()).availability())
        );
    }

    @Test
    void anUnknownIdIsNotFound() {
        assertEquals(Optional.empty(), people.person(people.nextPersonId()));
    }

    private Certification certification(String name) {
        Certification certification = new Certification(certifications.nextCertificationId(), new CertificationName(name));
        certifications.save(certification);
        return certification;
    }

    private Person person(String name) {
        return new Person(people.nextPersonId(), new PersonName(name), List.of(), Availability.always());
    }
}
