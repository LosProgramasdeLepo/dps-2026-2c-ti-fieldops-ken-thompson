package edu.itba.fieldops.api.catalog;

import edu.itba.fieldops.api.catalog.CatalogResponses.CertificationResponse;
import edu.itba.fieldops.api.catalog.CatalogResponses.ConsumableResponse;
import edu.itba.fieldops.api.catalog.CatalogResponses.InstrumentResponse;
import edu.itba.fieldops.api.catalog.CatalogResponses.PermitResponse;
import edu.itba.fieldops.api.catalog.CatalogResponses.PersonResponse;
import edu.itba.fieldops.api.catalog.CatalogResponses.VehicleResponse;
import edu.itba.fieldops.api.json.Periods;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.Vehicle;

public final class CatalogMapping {
    private CatalogMapping() {
    }

    public static CertificationResponse certification(Certification certification) {
        return new CertificationResponse(certification.id().value(), certification.name());
    }

    public static PersonResponse person(Person person) {
        return new PersonResponse(
                person.id().value(),
                person.name(),
                person.certifications().stream().map(CatalogMapping::certification).toList(),
                Periods.toResponse(person.availability())
        );
    }

    public static VehicleResponse vehicle(Vehicle vehicle) {
        return new VehicleResponse(vehicle.id().value(), vehicle.capacity().count(), Periods.toResponse(vehicle.availability()));
    }

    public static InstrumentResponse instrument(Instrument instrument) {
        return new InstrumentResponse(instrument.id().value(), instrument.kind().name(), Periods.toResponse(instrument.availability()));
    }

    public static ConsumableResponse consumable(Consumable consumable) {
        return new ConsumableResponse(consumable.id().value(), consumable.name(), consumable.stock().amount());
    }

    public static PermitResponse permit(Permit permit) {
        return new PermitResponse(permit.id().value(), permit.kind().name(), permit.zone().name(), Periods.toResponse(permit.validity()));
    }
}
