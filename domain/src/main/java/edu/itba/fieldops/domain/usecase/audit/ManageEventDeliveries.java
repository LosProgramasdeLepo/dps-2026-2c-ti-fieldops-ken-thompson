package edu.itba.fieldops.domain.usecase.audit;

import edu.itba.fieldops.domain.identity.EventId;

public interface ManageEventDeliveries {
    void retry(EventId eventId, String consumer);
}
