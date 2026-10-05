package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.identity.ExpeditionId;

public interface DraftExpedition {
    ExpeditionId draft(ExpeditionCharter charter);
}
