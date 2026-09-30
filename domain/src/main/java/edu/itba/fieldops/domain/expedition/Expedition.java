package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.itinerary.Itinerary;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class Expedition {
    private final ExpeditionId id;
    private final int version;
    private final ExpeditionId supersedes;
    private final ExpeditionCharter charter;
    private final Itinerary itinerary;
    private final Assignments assignments;
    private final List<PermitId> permits;
    private final AcceptedWarnings acceptedWarnings;
    private ExpeditionStatus status;

    static Expedition draft(ExpeditionId id, ExpeditionCharter charter) {
        return new Expedition(id, charter);
    }

    private Expedition(ExpeditionId id, ExpeditionCharter charter) {
        this.id = Objects.requireNonNull(id, "expedition id");
        this.version = 1;
        this.supersedes = null;
        this.charter = Objects.requireNonNull(charter, "charter");
        this.itinerary = new Itinerary();
        this.assignments = new Assignments();
        this.permits = new ArrayList<>();
        this.acceptedWarnings = new AcceptedWarnings();
        this.status = ExpeditionStatus.DRAFT;
    }

    private Expedition(ExpeditionId id, Expedition source) {
        this.id = Objects.requireNonNull(id, "expedition id");
        this.version = source.version + 1;
        this.supersedes = source.id;
        this.charter = source.charter;
        this.itinerary = source.itinerary.copy();
        this.assignments = source.assignments.copy();
        this.permits = new ArrayList<>(source.permits);
        this.acceptedWarnings = new AcceptedWarnings();
        this.status = ExpeditionStatus.DRAFT;
    }

    Expedition reviseAsDraft(ExpeditionId revisionId) {
        requireStatus(ExpeditionStatus.APPROVED, "revise");
        return new Expedition(revisionId, this);
    }

    void addActivity(Activity activity) {
        requireStatus(ExpeditionStatus.DRAFT, "add activity");
        Objects.requireNonNull(activity, "activity");
        requirePlanned(activity);
        itinerary.add(activity);
    }

    void addBlock(ActivityBlock block) {
        requireStatus(ExpeditionStatus.DRAFT, "add block");
        Objects.requireNonNull(block, "block");
        block.activities().forEach(this::requirePlanned);
        itinerary.add(block);
    }

    void removeActivity(ActivityId activityId) {
        requireStatus(ExpeditionStatus.DRAFT, "remove activity");
        itinerary.remove(activityId);
        assignments.removeActivity(activityId);
    }

    void addDependency(ActivityId activityId, ActivityId predecessorId) {
        requireStatus(ExpeditionStatus.DRAFT, "add dependency");
        itinerary.addDependency(activityId, predecessorId);
    }

    void addAssignment(Assignment assignment) {
        requireStatus(ExpeditionStatus.DRAFT, "assign resources");
        Objects.requireNonNull(assignment, "assignment");
        itinerary.activityOf(assignment.activityId());
        assignments.add(assignment);
    }

    void removeAssignment(Assignment assignment) {
        requireStatus(ExpeditionStatus.DRAFT, "unassign resources");
        Objects.requireNonNull(assignment, "assignment");
        assignments.remove(assignment);
    }

    void delay(ActivityId activityId, Duration delay) {
        requireStatus(ExpeditionStatus.DRAFT, "delay activity");
        Objects.requireNonNull(delay, "delay");
        if (delay.isNegative()) {
            throw new InvalidValue("delay must not be negative");
        }
        if (!delayFits(activityId, delay)) {
            throw new InvalidItinerary("activity window is outside the expedition period");
        }
        itinerary.delay(activityId, delay);
    }

    boolean delayFits(ActivityId activityId, Duration delay) {
        return itinerary.delayed(activityId, delay).stream()
                .allMatch(activity -> charter.period().contains(activity.window()));
    }

    void addPermit(PermitId permitId) {
        requireStatus(ExpeditionStatus.DRAFT, "attach permit");
        Objects.requireNonNull(permitId, "permit id");
        if (permits.contains(permitId)) {
            throw new InvalidValue("duplicate permit: " + permitId);
        }
        permits.add(permitId);
    }

    void acceptWarning(AcceptedWarning warning) {
        requireStatus(ExpeditionStatus.IN_REVIEW, "accept warning");
        Objects.requireNonNull(warning, "warning");
        if (!charter.isResponsible(warning.acceptedBy())) {
            throw new InvalidValue("warning must be accepted by a responsible");
        }
        acceptedWarnings.accept(warning);
    }

    void submitForReview() {
        if (itinerary.activities().isEmpty()) {
            throw new InvalidItinerary("expedition has no activities");
        }
        transition(ExpeditionStatus.DRAFT, ExpeditionStatus.IN_REVIEW, "submit for review");
    }

    void returnToDraft() {
        requireStatus(ExpeditionStatus.IN_REVIEW, "return to draft");
        status = ExpeditionStatus.DRAFT;
        acceptedWarnings.clear();
    }

    void markApproved() {
        transition(ExpeditionStatus.IN_REVIEW, ExpeditionStatus.APPROVED, "approve");
    }

    void markSuperseded() {
        transition(ExpeditionStatus.APPROVED, ExpeditionStatus.SUPERSEDED, "supersede");
    }

    boolean hasAccepted(ValidationIssue warning) {
        return acceptedWarnings.covers(warning);
    }

    public ExpeditionId id() {
        return id;
    }

    public int version() {
        return version;
    }

    public Optional<ExpeditionId> supersedes() {
        return Optional.ofNullable(supersedes);
    }

    public ExpeditionCharter charter() {
        return charter;
    }

    public ExpeditionStatus status() {
        return status;
    }

    public List<Activity> activities() {
        return itinerary.activities();
    }

    public List<ItineraryItem> items() {
        return itinerary.items();
    }

    public List<ActivityBlock> blocks() {
        return itinerary.blocks();
    }

    public Activity activityOf(ActivityId activityId) {
        return itinerary.activityOf(activityId);
    }

    public Set<ActivityId> predecessorsOf(ActivityId activityId) {
        return itinerary.predecessorsOf(activityId);
    }

    public Duration duration(Function<Activity, Duration> leafDuration) {
        return itinerary.duration(leafDuration);
    }

    public Duration estimatedDuration() {
        return itinerary.estimatedDuration();
    }

    public RiskLevel estimatedRisk() {
        return itinerary.risk();
    }

    public Map<ConsumableId, Stock> estimatedConsumption() {
        return itinerary.estimatedConsumption();
    }

    public Assignments assignments() {
        return assignments.copy();
    }

    public List<PermitId> permits() {
        return List.copyOf(permits);
    }

    public List<AcceptedWarning> acceptedWarnings() {
        return acceptedWarnings.all();
    }

    private void requirePlanned(Activity activity) {
        if (!charter.zones().contains(activity.zone())) {
            throw new InvalidItinerary("activity zone is not part of the expedition");
        }
        if (!charter.period().contains(activity.window())) {
            throw new InvalidItinerary("activity window is outside the expedition period");
        }
    }

    private void transition(ExpeditionStatus from, ExpeditionStatus to, String action) {
        requireStatus(from, action);
        status = to;
    }

    private void requireStatus(ExpeditionStatus expected, String action) {
        if (status != expected) {
            throw new InvalidExpeditionTransition(status, action);
        }
    }
}
