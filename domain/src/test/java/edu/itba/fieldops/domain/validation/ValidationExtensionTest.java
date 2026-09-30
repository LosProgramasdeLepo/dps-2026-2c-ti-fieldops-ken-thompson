package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Consumables;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Instruments;
import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Permits;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.catalog.Vehicles;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionEditing;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.OccupyingExpeditions;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.PlanningContext;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationExtensionTest {
    private static final Instant DAY = Instant.parse("2026-03-02T06:00:00Z");
    private static final WorkZone ZONE = new WorkZone("Glaciar Norte");

    @Test
    void acceptsARuleThatTheValidatorDoesNotKnowAbout() {
        Expedition expedition = expeditionWithTransit();
        ValidationRule everyActivityNeedsABriefing = context -> context.plan().activities().stream()
                .map(activity -> new ValidationIssue(
                        IssueSeverity.WARNING,
                        "BRIEFING",
                        "activity " + activity.name() + " has no safety briefing"
                ))
                .toList();

        ValidationResult result = new RuleBasedValidator(List.of(everyActivityNeedsABriefing))
                .validate(new PlanningContext(expedition, emptyCatalog(), OccupyingExpeditions.of(expedition, List.of(), Map.of())));

        assertEquals(1, result.issues().size());
        assertEquals("BRIEFING", result.issues().getFirst().code());
    }

    @Test
    void combinesACustomRuleWithAStandardRule() {
        Expedition expedition = expeditionWithTransit();
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(expedition.activities().getFirst().id(), new PersonId(UUID.randomUUID())));
        ValidationRule alwaysCritical = context -> List.of(
                new ValidationIssue(IssueSeverity.CRITICAL, "BRIEFING", "no briefing on file")
        );

        ValidationResult result = new RuleBasedValidator(List.of(new MissingResourceRule(), alwaysCritical))
                .validate(new PlanningContext(expedition, emptyCatalog(), OccupyingExpeditions.of(expedition, List.of(), Map.of())));

        assertTrue(result.hasCritical());
        assertTrue(hasCode(result, "RESOURCE"), () -> "expected RESOURCE in " + result.issues());
        assertTrue(hasCode(result, "BRIEFING"), () -> "expected BRIEFING in " + result.issues());
    }

    @Test
    void doesNotObserveRulesAddedAfterConstruction() {
        Expedition expedition = expeditionWithTransit();
        ValidationRule noise = context -> List.of(
                new ValidationIssue(IssueSeverity.WARNING, "NOISE", "should not reach the validator")
        );
        List<ValidationRule> mutable = new ArrayList<>();
        RuleBasedValidator validator = new RuleBasedValidator(mutable);
        mutable.add(noise);

        ValidationResult result = validator.validate(new PlanningContext(expedition, emptyCatalog(), OccupyingExpeditions.of(expedition, List.of(), Map.of())));

        assertFalse(hasCode(result, "NOISE"), () -> "validator kept a live view of the rule list: " + result.issues());
    }

    private static boolean hasCode(ValidationResult result, String code) {
        return result.issues().stream().anyMatch(issue -> issue.code().equals(code));
    }

    private static Expedition expeditionWithTransit() {
        Expedition expedition = ExpeditionEditing.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("relevar el frente del glaciar")),
                        new TimePeriod(DAY, DAY.plusSeconds(24 * 3600L)),
                        List.of(ZONE),
                        List.of(new PersonId(UUID.randomUUID())),
                        List.of(new Restriction("sin vuelos nocturnos"))
                )
        );
        ExpeditionEditing.addActivity(expedition, Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "Traslado al campamento")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(ZONE, new TimePeriod(DAY, DAY.plusSeconds(4 * 3600L)))
                .build());
        return expedition;
    }

    private static Catalogs emptyCatalog() {
        Empty empty = new Empty();
        return new Catalogs(new BookableResources(empty, empty, empty), empty, empty);
    }

    private static final class Empty implements People, Vehicles, Instruments, Consumables, Permits {
        @Override
        public Optional<Person> person(PersonId id) {
            return Optional.empty();
        }

        @Override
        public List<Person> people() {
            return List.of();
        }

        @Override
        public Optional<Vehicle> vehicle(VehicleId id) {
            return Optional.empty();
        }

        @Override
        public List<Vehicle> vehicles() {
            return List.of();
        }

        @Override
        public Optional<Instrument> instrument(InstrumentId id) {
            return Optional.empty();
        }

        @Override
        public List<Instrument> instruments() {
            return List.of();
        }

        @Override
        public Optional<Consumable> consumable(ConsumableId id) {
            return Optional.empty();
        }

        @Override
        public Optional<Permit> permit(PermitId id) {
            return Optional.empty();
        }
    }
}
