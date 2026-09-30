package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.Incident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplanProposalTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");

    @Test
    void acceptRecordsTheResponsible() {
        PersonId ada = new PersonId(UUID.randomUUID());
        ReplanProposal proposal = pending();

        proposal.accept(ada, DAY);

        assertEquals(ReplanProposal.Decision.ACCEPTED, proposal.decision());
        assertEquals(Optional.of(ada), proposal.decidedBy());
        assertEquals(Optional.of(DAY), proposal.decidedAt());
    }

    @Test
    void rejectKeepsTheIncident() {
        ReplanProposal proposal = pending();
        Incident incident = proposal.incident();

        proposal.reject(new PersonId(UUID.randomUUID()), DAY);

        assertEquals(ReplanProposal.Decision.REJECTED, proposal.decision());
        assertEquals(incident, proposal.incident());
    }

    @Test
    void cannotAcceptTwice() {
        ReplanProposal proposal = pending();
        proposal.accept(new PersonId(UUID.randomUUID()), DAY);

        assertThrows(
                InvalidValue.class,
                () -> proposal.accept(new PersonId(UUID.randomUUID()), DAY.plus(Duration.ofHours(1)))
        );
    }

    @Test
    void incidentWithoutActivityIsRejected() {
        assertThrows(
                InvalidValue.class,
                () -> new ReplanProposal(
                        new ProposalId(UUID.randomUUID()),
                        new ExpeditionId(UUID.randomUUID()),
                        Incident.of("storm", DAY),
                        draft()
                )
        );
    }

    private static ReplanProposal pending() {
        return new ReplanProposal(
                new ProposalId(UUID.randomUUID()),
                new ExpeditionId(UUID.randomUUID()),
                new Incident("storm on site", DAY, new ActivityId(UUID.randomUUID())),
                draft()
        );
    }

    private static Expedition draft() {
        return Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                List.of(new Objective("Map wetland")),
                new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                List.of(new WorkZone("Delta")),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("Daylight only"))
        );
    }
}
