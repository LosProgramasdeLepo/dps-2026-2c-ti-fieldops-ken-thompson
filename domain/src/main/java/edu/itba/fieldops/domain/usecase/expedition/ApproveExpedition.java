package edu.itba.fieldops.domain.usecase.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;

public interface ApproveExpedition {
    void approve(ExpeditionId expeditionId);
}
