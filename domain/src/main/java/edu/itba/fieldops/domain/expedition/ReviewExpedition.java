package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.identity.ExpeditionId;

public interface ReviewExpedition {
    void submit(ExpeditionId expeditionId);

    ValidationResult validate(ExpeditionId expeditionId);

    void acceptWarning(ExpeditionId expeditionId, AcceptedWarning warning);

    void returnToDraft(ExpeditionId expeditionId);
}
