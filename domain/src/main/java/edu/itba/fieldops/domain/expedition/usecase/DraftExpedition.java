package edu.itba.fieldops.domain.expedition.usecase;

import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.identity.ExpeditionId;

public interface DraftExpedition {
    ExpeditionId draft(ExpeditionCharter charter);
}
