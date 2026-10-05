package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;

public interface ConsultExpedition {
    PlanSnapshot of(ExpeditionId expeditionId);
}
