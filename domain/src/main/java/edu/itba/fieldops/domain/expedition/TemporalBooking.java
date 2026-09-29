package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public sealed interface TemporalBooking
        permits TemporalBooking.PersonBooking, TemporalBooking.VehicleBooking, TemporalBooking.InstrumentBooking {

    ActivityId activityId();

    TimePeriod window();

    String label();

    boolean availableIn(Catalog catalog);

    boolean unavailableIn(Catalog catalog);

    boolean conflicts(TemporalBooking other);

    static List<TemporalBooking> of(Expedition expedition) {
        List<TemporalBooking> bookings = new ArrayList<>();
        for (Assignment assignment : expedition.assignments().all()) {
            assignment.booking(expedition.activityOf(assignment.activityId()).window()).ifPresent(bookings::add);
        }
        return bookings;
    }

    record PersonBooking(PersonId personId, ActivityId activityId, TimePeriod window) implements TemporalBooking {
        public PersonBooking {
            Objects.requireNonNull(personId, "person id");
            Objects.requireNonNull(activityId, "activity id");
            Objects.requireNonNull(window, "window");
        }

        @Override
        public String label() {
            return "person " + personId.value();
        }

        @Override
        public boolean availableIn(Catalog catalog) {
            return isAvailable(catalog.person(personId).map(person -> person.availableDuring(window)));
        }

        @Override
        public boolean unavailableIn(Catalog catalog) {
            return isUnavailable(catalog.person(personId).map(person -> person.availableDuring(window)));
        }

        @Override
        public boolean conflicts(TemporalBooking other) {
            return other instanceof PersonBooking person
                    && personId.equals(person.personId)
                    && window.overlaps(person.window);
        }
    }

    record VehicleBooking(VehicleId vehicleId, ActivityId activityId, TimePeriod window) implements TemporalBooking {
        public VehicleBooking {
            Objects.requireNonNull(vehicleId, "vehicle id");
            Objects.requireNonNull(activityId, "activity id");
            Objects.requireNonNull(window, "window");
        }

        @Override
        public String label() {
            return "vehicle " + vehicleId.value();
        }

        @Override
        public boolean availableIn(Catalog catalog) {
            return isAvailable(catalog.vehicle(vehicleId).map(vehicle -> vehicle.availableDuring(window)));
        }

        @Override
        public boolean unavailableIn(Catalog catalog) {
            return isUnavailable(catalog.vehicle(vehicleId).map(vehicle -> vehicle.availableDuring(window)));
        }

        @Override
        public boolean conflicts(TemporalBooking other) {
            return other instanceof VehicleBooking vehicle
                    && vehicleId.equals(vehicle.vehicleId)
                    && window.overlaps(vehicle.window);
        }
    }

    record InstrumentBooking(InstrumentId instrumentId, ActivityId activityId, TimePeriod window) implements TemporalBooking {
        public InstrumentBooking {
            Objects.requireNonNull(instrumentId, "instrument id");
            Objects.requireNonNull(activityId, "activity id");
            Objects.requireNonNull(window, "window");
        }

        @Override
        public String label() {
            return "instrument " + instrumentId.value();
        }

        @Override
        public boolean availableIn(Catalog catalog) {
            return isAvailable(catalog.instrument(instrumentId).map(instrument -> instrument.availableDuring(window)));
        }

        @Override
        public boolean unavailableIn(Catalog catalog) {
            return isUnavailable(catalog.instrument(instrumentId).map(instrument -> instrument.availableDuring(window)));
        }

        @Override
        public boolean conflicts(TemporalBooking other) {
            return other instanceof InstrumentBooking instrument
                    && instrumentId.equals(instrument.instrumentId)
                    && window.overlaps(instrument.window);
        }
    }

    private static boolean isAvailable(Optional<Boolean> available) {
        return available.orElse(false);
    }

    private static boolean isUnavailable(Optional<Boolean> available) {
        return available.filter(ready -> !ready).isPresent();
    }
}
