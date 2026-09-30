package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface TemporalBooking {
    ActivityId activityId();

    TimePeriod window();

    String label();

    boolean availableIn(BookableResources resources);

    boolean unavailableIn(BookableResources resources);

    boolean conflicts(TemporalBooking other);

    default boolean conflictsWith(PersonId personId, TimePeriod window) {
        return false;
    }

    default boolean conflictsWith(VehicleId vehicleId, TimePeriod window) {
        return false;
    }

    default boolean conflictsWith(InstrumentId instrumentId, TimePeriod window) {
        return false;
    }

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
        public boolean availableIn(BookableResources resources) {
            return isAvailable(resources.people().person(personId).map(person -> person.availableDuring(window)));
        }

        @Override
        public boolean unavailableIn(BookableResources resources) {
            return isUnavailable(resources.people().person(personId).map(person -> person.availableDuring(window)));
        }

        @Override
        public boolean conflicts(TemporalBooking other) {
            return other.conflictsWith(personId, window);
        }

        @Override
        public boolean conflictsWith(PersonId otherPerson, TimePeriod otherWindow) {
            return personId.equals(otherPerson) && window.overlaps(otherWindow);
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
        public boolean availableIn(BookableResources resources) {
            return isAvailable(resources.vehicles().vehicle(vehicleId).map(vehicle -> vehicle.availableDuring(window)));
        }

        @Override
        public boolean unavailableIn(BookableResources resources) {
            return isUnavailable(resources.vehicles().vehicle(vehicleId).map(vehicle -> vehicle.availableDuring(window)));
        }

        @Override
        public boolean conflicts(TemporalBooking other) {
            return other.conflictsWith(vehicleId, window);
        }

        @Override
        public boolean conflictsWith(VehicleId otherVehicle, TimePeriod otherWindow) {
            return vehicleId.equals(otherVehicle) && window.overlaps(otherWindow);
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
        public boolean availableIn(BookableResources resources) {
            return isAvailable(resources.instruments().instrument(instrumentId).map(instrument -> instrument.availableDuring(window)));
        }

        @Override
        public boolean unavailableIn(BookableResources resources) {
            return isUnavailable(resources.instruments().instrument(instrumentId).map(instrument -> instrument.availableDuring(window)));
        }

        @Override
        public boolean conflicts(TemporalBooking other) {
            return other.conflictsWith(instrumentId, window);
        }

        @Override
        public boolean conflictsWith(InstrumentId otherInstrument, TimePeriod otherWindow) {
            return instrumentId.equals(otherInstrument) && window.overlaps(otherWindow);
        }
    }

    private static boolean isAvailable(Optional<Boolean> available) {
        return available.orElse(false);
    }

    private static boolean isUnavailable(Optional<Boolean> available) {
        return available.filter(ready -> !ready).isPresent();
    }
}
