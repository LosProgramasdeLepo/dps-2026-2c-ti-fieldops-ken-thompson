package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;

import java.util.List;
import java.util.Optional;

public interface Catalog {
    Optional<Person> person(PersonId id);

    Optional<Vehicle> vehicle(VehicleId id);

    Optional<Instrument> instrument(InstrumentId id);

    Optional<Consumable> consumable(ConsumableId id);

    Optional<Permit> permit(PermitId id);

    List<Person> people();

    List<Vehicle> vehicles();

    List<Instrument> instruments();
}
