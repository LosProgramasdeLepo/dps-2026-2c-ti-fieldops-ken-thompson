package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.List;
import java.util.Objects;

public record ExpeditionCharter(
        List<Objective> objectives,
        TimePeriod period,
        List<WorkZone> zones,
        List<PersonId> responsibles,
        List<Restriction> restrictions
) {
    public ExpeditionCharter {
        objectives = required(objectives, "objectives");
        Objects.requireNonNull(period, "period");
        zones = required(zones, "zones");
        responsibles = required(responsibles, "responsibles");
        restrictions = List.copyOf(restrictions);
    }

    public boolean isResponsible(PersonId personId) {
        return responsibles.contains(personId);
    }

    private static <T> List<T> required(List<T> values, String name) {
        if (values == null || values.isEmpty()) {
            throw new InvalidValue(name + " must not be empty");
        }
        return List.copyOf(values);
    }
}
