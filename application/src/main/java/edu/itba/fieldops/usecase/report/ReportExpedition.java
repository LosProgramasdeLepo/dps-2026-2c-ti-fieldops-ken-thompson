package edu.itba.fieldops.usecase.report;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.report.OperationalReport;

public interface ReportExpedition {
    OperationalReport of(ExpeditionId expeditionId);
}
