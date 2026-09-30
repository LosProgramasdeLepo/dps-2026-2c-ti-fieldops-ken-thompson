package edu.itba.fieldops.domain.expedition.usecase;

import edu.itba.fieldops.domain.identity.ExpeditionId;

public interface ConsultExpedition {
    PlanSnapshot of(ExpeditionId expeditionId);
}
