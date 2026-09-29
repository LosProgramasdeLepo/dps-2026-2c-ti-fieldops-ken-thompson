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
import java.util.UUID;

public record TemporalBooking(Kind kind, UUID resourceId, ActivityId activityId, TimePeriod window) {
    public TemporalBooking {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(resourceId, "resource id");
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(window, "window");
    }

    public static List<TemporalBooking> of(Expedition expedition) {
        List<TemporalBooking> bookings = new ArrayList<>();
        for (Assignment assignment : expedition.assignments().all()) {
            assignment.booking(expedition.activityOf(assignment.activityId()).window()).ifPresent(bookings::add);
        }
        return bookings;
    }

    public boolean availableIn(Catalog catalog) {
        return presence(catalog) == Presence.AVAILABLE;
    }

    public boolean unavailableIn(Catalog catalog) {
        return presence(catalog) == Presence.UNAVAILABLE;
    }

    private Presence presence(Catalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        return kind.in(catalog, resourceId, window);
    }

    public boolean conflicts(TemporalBooking other) {
        return conflicts(other.kind, other.resourceId, other.window);
    }

    boolean conflicts(Kind otherKind, UUID otherId, TimePeriod otherWindow) {
        return kind == otherKind && resourceId.equals(otherId) && window.overlaps(otherWindow);
    }

    public String label() {
        return kind.name().toLowerCase() + " " + resourceId;
    }

    public enum Kind {
        PERSON {
            @Override
            Presence in(Catalog catalog, UUID resourceId, TimePeriod window) {
                return listed(catalog.person(new PersonId(resourceId)).map(person -> person.availableDuring(window)));
            }
        },
        VEHICLE {
            @Override
            Presence in(Catalog catalog, UUID resourceId, TimePeriod window) {
                return listed(catalog.vehicle(new VehicleId(resourceId)).map(vehicle -> vehicle.availableDuring(window)));
            }
        },
        INSTRUMENT {
            @Override
            Presence in(Catalog catalog, UUID resourceId, TimePeriod window) {
                return listed(catalog.instrument(new InstrumentId(resourceId)).map(instrument -> instrument.availableDuring(window)));
            }
        };

        abstract Presence in(Catalog catalog, UUID resourceId, TimePeriod window);

        private static Presence listed(Optional<Boolean> available) {
            return available
                    .map(isAvailable -> isAvailable ? Presence.AVAILABLE : Presence.UNAVAILABLE)
                    .orElse(Presence.UNKNOWN);
        }
    }

    private enum Presence { UNKNOWN, AVAILABLE, UNAVAILABLE }
}
