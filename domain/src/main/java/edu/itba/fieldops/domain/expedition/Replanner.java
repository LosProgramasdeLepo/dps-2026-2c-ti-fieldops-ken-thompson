package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.identity.ActivityId;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Replanner {
    private final AssignmentSuggester suggester;

    public Replanner(AssignmentSuggester suggester) {
        this.suggester = Objects.requireNonNull(suggester, "assignment suggester");
    }

    public Expedition cancel(Expedition expedition, ActivityId activityId, Catalog catalog, OccupyingExpeditions peers) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(peers, "peers");
        Expedition plan = editablePlanFor(expedition);
        plan.removeActivity(activityId);
        refill(plan, catalog, peers);
        return plan;
    }

    public Expedition delay(
            Expedition expedition,
            ActivityId activityId,
            Duration delay,
            Catalog catalog,
            OccupyingExpeditions peers
    ) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(peers, "peers");
        Expedition plan = editablePlanFor(expedition);
        plan.delay(activityId, delay);
        dropInvalid(plan, catalog, peers);
        refill(plan, catalog, peers);
        return plan;
    }

    public Expedition replaceUnavailable(Expedition expedition, Catalog catalog, OccupyingExpeditions peers) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(peers, "peers");
        Expedition plan = revisionOrSelf(expedition);
        dropInvalid(plan, catalog, peers);
        refill(plan, catalog, peers);
        return plan;
    }

    private static Expedition editablePlanFor(Expedition expedition) {
        Expedition plan = revisionOrSelf(expedition);
        if (plan.status() != ExpeditionStatus.DRAFT) {
            plan.returnToDraft();
        }
        return plan;
    }

    private static Expedition revisionOrSelf(Expedition expedition) {
        return expedition.status().hasBeenApproved() ? expedition.reviseAsDraft() : expedition;
    }

    private void refill(Expedition expedition, Catalog catalog, OccupyingExpeditions peers) {
        for (Assignment assignment : suggester.suggest(expedition, catalog, peers)) {
            expedition.addAssignment(assignment);
        }
    }

    private static void dropInvalid(Expedition expedition, Catalog catalog, OccupyingExpeditions peers) {
        List<TemporalBooking> occupying = new ArrayList<>();
        for (Expedition peer : peers.plans()) {
            occupying.addAll(TemporalBooking.of(peer));
        }
        List<TemporalBooking> kept = new ArrayList<>();
        List<Assignment> drop = new ArrayList<>();
        for (Assignment assignment : expedition.assignments().all()) {
            Optional<TemporalBooking> booking = assignment.booking(
                    expedition.activityOf(assignment.activityId()).window()
            );
            if (booking.isEmpty()) {
                continue;
            }
            TemporalBooking slot = booking.get();
            boolean invalid = !slot.availableIn(catalog)
                    || occupying.stream().anyMatch(slot::conflicts)
                    || kept.stream().anyMatch(slot::conflicts);
            if (invalid) {
                drop.add(assignment);
            } else {
                kept.add(slot);
            }
        }
        for (Assignment assignment : drop) {
            expedition.removeAssignment(assignment);
        }
    }
}
