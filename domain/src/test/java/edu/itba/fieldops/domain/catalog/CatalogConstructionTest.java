package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogConstructionTest {
    private static final TimePeriod WEEK = new TimePeriod(
            Instant.parse("2026-11-01T00:00:00Z"),
            Instant.parse("2026-11-08T00:00:00Z")
    );

    @Test
    void personHoldsCertificationAndAvailability() {
        Certification sampling = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        Person person = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(sampling), Availability.always());

        assertAll(
                () -> assertTrue(person.holds(sampling.id())),
                () -> assertFalse(person.holds(new CertificationId(UUID.randomUUID()))),
                () -> assertTrue(person.availableDuring(WEEK))
        );
    }

    @Test
    void consumableComparesAgainstStock() {
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(20));

        assertAll(
                () -> assertTrue(vials.hasAtLeast(new Stock(20))),
                () -> assertFalse(vials.hasAtLeast(new Stock(21)))
        );
    }

    @Test
    void permitCoversMatchingZoneAndWindow() {
        Permit permit = Permit.zone(new PermitId(UUID.randomUUID()), new WorkZone("Delta"), WEEK);

        Permit night = Permit.night(new PermitId(UUID.randomUUID()), new WorkZone("Delta"), WEEK);

        assertAll(
                () -> assertTrue(permit.covers(new WorkZone("Delta"), WEEK)),
                () -> assertFalse(permit.covers(new WorkZone("Coast"), WEEK)),
                () -> assertFalse(permit.nightOperation()),
                () -> assertTrue(night.nightOperation()),
                () -> assertTrue(night.covers(new WorkZone("Delta"), WEEK))
        );
    }

    @Test
    void availabilityCoversWhenAPeriodContainsTheWindow() {
        Availability availability = new Availability(List.of(WEEK));
        TimePeriod december = new TimePeriod(
                Instant.parse("2026-12-01T00:00:00Z"),
                Instant.parse("2026-12-02T00:00:00Z")
        );

        assertAll(
                () -> assertTrue(availability.covers(WEEK)),
                () -> assertFalse(availability.covers(december))
        );
    }
}
