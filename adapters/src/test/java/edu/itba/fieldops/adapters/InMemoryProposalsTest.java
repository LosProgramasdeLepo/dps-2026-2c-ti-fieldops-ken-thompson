package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.identity.IncidentId;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryProposalsTest {
    @Test
    void anIncidentWithoutAProposalFindsNothing() {
        assertEquals(Optional.empty(), new InMemoryReplanProposalRepository().forIncident(new IncidentId(UUID.randomUUID())));
    }
}
