package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.StoredPeople;
import edu.itba.fieldops.domain.support.StoredPlans;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RevisionsTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Test
    void aRootWithoutRevisionsIsItsOwnFamily() {
        Expedition root = approvedRoot();

        assertEquals(Set.of(root.id()), Revisions.family(root, List.of(root, approvedRoot())));
    }

    @Test
    void aChainOfRevisionsFormsOneFamilyFromAnyMember() {
        Expedition root = approvedRoot();
        Expedition second = StoredPlans.approved(root.reviseAsDraft(newId()));
        Expedition third = second.reviseAsDraft(newId());
        Expedition stranger = approvedRoot();
        List<Expedition> all = List.of(root, second, third, stranger);
        Set<ExpeditionId> expected = Set.of(root.id(), second.id(), third.id());

        assertEquals(expected, Revisions.family(root, all));
        assertEquals(expected, Revisions.family(second, all));
        assertEquals(expected, Revisions.family(third, all));
    }

    @Test
    void draftsAndBranchesBelongToTheFamily() {
        Expedition root = approvedRoot();
        Expedition draft = root.reviseAsDraft(newId());
        Expedition branch = root.reviseAsDraft(newId());
        List<Expedition> all = List.of(root, draft, branch);

        assertEquals(Set.of(root.id(), draft.id(), branch.id()), Revisions.family(draft, all));
    }

    private static ExpeditionId newId() {
        return new ExpeditionId(UUID.randomUUID());
    }

    private static Expedition approvedRoot() {
        PersonId lead = new PersonId(UUID.randomUUID());
        Expedition plan = Expedition.draft(
                newId(),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                        List.of(DELTA),
                        List.of(lead),
                        List.of()
                ),
                StoredPeople.of(new Person(lead, new PersonName("Ada"), List.of(), Availability.always()))
        );
        plan.addActivity(Activity.sampling(new CertificationId(UUID.randomUUID()))
                .named(new ActivityId(UUID.randomUUID()), new ActivityName("Soil sampling"))
                .estimated(new EstimatedDuration(Duration.ofHours(4)), RiskLevel.MEDIUM)
                .in(DELTA, new TimePeriod(DAY, DAY.plus(Duration.ofHours(4))))
                .build());
        return StoredPlans.approved(plan);
    }
}
