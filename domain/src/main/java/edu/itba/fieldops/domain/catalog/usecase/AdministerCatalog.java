package edu.itba.fieldops.domain.catalog.usecase;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.List;

public interface AdministerCatalog {
    CertificationId registerCertification(String name);

    PersonId registerPerson(String name, List<CertificationId> certifications, Availability availability);

    VehicleId registerVehicle(Passengers capacity, Availability availability);

    InstrumentId registerInstrument(InstrumentKind kind, Availability availability);

    ConsumableId registerConsumable(String name, Stock stock);

    PermitId registerPermit(PermitKind kind, WorkZone zone, TimePeriod validity);

    void changeAvailability(PersonId personId, Availability availability);

    void changeAvailability(VehicleId vehicleId, Availability availability);

    void changeAvailability(InstrumentId instrumentId, Availability availability);

    void certify(PersonId personId, CertificationId certificationId);

    void changeStock(ConsumableId consumableId, Stock stock);
}
