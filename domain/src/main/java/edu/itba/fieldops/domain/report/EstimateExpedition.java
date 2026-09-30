package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.identity.ExpeditionId;

public interface EstimateExpedition {
    Estimate of(ExpeditionId expeditionId);
}
