package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;

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

    public void cancel(Expedition expedition, ActivityId activityId, BookableResources resources, OccupyingExpeditions peers) {
        requireDraft(expedition);
        expedition.removeActivity(activityId);
        refill(expedition, resources, peers);
    }

    public void delay(
            Expedition expedition,
            ActivityId activityId,
            Duration delay,
            BookableResources resources,
            OccupyingExpeditions peers
    ) {
        requireDraft(expedition);
        expedition.delay(activityId, delay);
        dropInvalid(expedition, resources, peers);
        refill(expedition, resources, peers);
    }

    public void replaceUnavailable(Expedition expedition, BookableResources resources, OccupyingExpeditions peers) {
        requireDraft(expedition);
        dropInvalid(expedition, resources, peers);
        refill(expedition, resources, peers);
    }

    private static void requireDraft(Expedition expedition) {
        if (expedition.status() == ExpeditionStatus.IN_REVIEW) {
            expedition.returnToDraft();
        }
        if (expedition.status() != ExpeditionStatus.DRAFT) {
            throw new InvalidExpeditionTransition(expedition.status(), "replan");
        }
    }

    private void refill(Expedition expedition, BookableResources resources, OccupyingExpeditions peers) {
        for (Assignment assignment : suggester.suggest(expedition, resources, peers)) {
            expedition.addAssignment(assignment);
        }
    }

    private static void dropInvalid(Expedition expedition, BookableResources resources, OccupyingExpeditions peers) {
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
            boolean invalid = !slot.availableIn(resources)
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
