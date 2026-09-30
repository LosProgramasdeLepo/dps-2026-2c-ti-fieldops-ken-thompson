package edu.itba.fieldops.domain.report.usecase;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.report.OperationalReport;

public interface ReportExpedition {
    OperationalReport of(ExpeditionId expeditionId);
}
