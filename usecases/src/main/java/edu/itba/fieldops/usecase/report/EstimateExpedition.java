package edu.itba.fieldops.usecase.report;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.report.Estimate;

public interface EstimateExpedition {
    Estimate of(ExpeditionId expeditionId);
}
