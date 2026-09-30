package edu.itba.fieldops.domain.expedition.usecase;

import edu.itba.fieldops.domain.identity.ExpeditionId;

public interface ApproveExpedition {
    void approve(ExpeditionId expeditionId);
}
