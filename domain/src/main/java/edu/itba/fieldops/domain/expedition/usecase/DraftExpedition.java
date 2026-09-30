package edu.itba.fieldops.domain.expedition.usecase;

import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.List;

public interface DraftExpedition {
    ExpeditionId draft(
            List<Objective> objectives,
            TimePeriod period,
            List<WorkZone> zones,
            List<PersonId> responsibles,
            List<Restriction> restrictions
    );
}
