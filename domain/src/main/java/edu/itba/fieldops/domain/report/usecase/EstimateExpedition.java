package edu.itba.fieldops.domain.report.usecase;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.report.Estimate;

public interface EstimateExpedition {
    Estimate of(ExpeditionId expeditionId);
}
