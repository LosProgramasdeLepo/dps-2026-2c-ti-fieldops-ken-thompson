package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.identity.ExpeditionId;

public interface ReportExpedition {
    OperationalReport of(ExpeditionId expeditionId);
}
