package edu.itba.fieldops.domain.usecase.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;

public interface RespondToIncident {
    void respondTo(ExpeditionId runId, IncidentId incidentId);
}
