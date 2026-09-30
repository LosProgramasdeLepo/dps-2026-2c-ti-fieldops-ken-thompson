package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;

import java.time.Duration;
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
    private final PlanContent content;
    private final AcceptedWarnings acceptedWarnings = new AcceptedWarnings();
    private ExpeditionStatus status = ExpeditionStatus.DRAFT;

    static Expedition draft(ExpeditionId id, ExpeditionCharter charter) {
        return new Expedition(id, charter);
    }

    private Expedition(ExpeditionId id, ExpeditionCharter charter) {
        this.id = Objects.requireNonNull(id, "expedition id");
        this.version = 1;
        this.supersedes = null;
        this.content = new PlanContent(charter);
    }

    private Expedition(ExpeditionId id, Expedition source) {
        this.id = Objects.requireNonNull(id, "expedition id");
        this.version = source.version + 1;
        this.supersedes = source.id;
        this.content = source.content.copy();
    }

    Expedition reviseAsDraft(ExpeditionId revisionId) {
        requireStatus(ExpeditionStatus.APPROVED, "revise");
        return new Expedition(revisionId, this);
    }

    void addActivity(Activity activity) {
        requireStatus(ExpeditionStatus.DRAFT, "add activity");
        content.addActivity(activity);
    }

    void addBlock(ActivityBlock block) {
        requireStatus(ExpeditionStatus.DRAFT, "add block");
        content.addBlock(block);
    }

    void removeActivity(ActivityId activityId) {
        requireStatus(ExpeditionStatus.DRAFT, "remove activity");
        content.removeActivity(activityId);
    }

    void addDependency(ActivityId activityId, ActivityId predecessorId) {
        requireStatus(ExpeditionStatus.DRAFT, "add dependency");
        content.addDependency(activityId, predecessorId);
    }

    void addAssignment(Assignment assignment) {
        requireStatus(ExpeditionStatus.DRAFT, "assign resources");
        content.addAssignment(assignment);
    }

    void removeAssignment(Assignment assignment) {
        requireStatus(ExpeditionStatus.DRAFT, "unassign resources");
        content.removeAssignment(assignment);
    }

    void delay(ActivityId activityId, Duration delay) {
        requireStatus(ExpeditionStatus.DRAFT, "delay activity");
        content.delay(activityId, delay);
    }

    boolean delayFits(ActivityId activityId, Duration delay) {
        return content.delayFits(activityId, delay);
    }

    void addPermit(PermitId permitId) {
        requireStatus(ExpeditionStatus.DRAFT, "attach permit");
        content.addPermit(permitId);
    }

    void acceptWarning(AcceptedWarning warning) {
        requireStatus(ExpeditionStatus.IN_REVIEW, "accept warning");
        if (!charter().isResponsible(Objects.requireNonNull(warning, "warning").acceptedBy())) {
            throw new InvalidValue("warning must be accepted by a responsible");
        }
        acceptedWarnings.accept(warning);
    }

    void submitForReview() {
        if (activities().isEmpty()) {
            throw new InvalidItinerary("expedition has no activities");
        }
        transition(ExpeditionStatus.DRAFT, ExpeditionStatus.IN_REVIEW, "submit for review");
    }

    void returnToDraft() {
        transition(ExpeditionStatus.IN_REVIEW, ExpeditionStatus.DRAFT, "return to draft");
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

    public ExpeditionStatus status() {
        return status;
    }

    public List<AcceptedWarning> acceptedWarnings() {
        return acceptedWarnings.all();
    }

    public ExpeditionCharter charter() {
        return content.charter();
    }

    public List<Activity> activities() {
        return content.activities();
    }

    public List<ItineraryItem> items() {
        return content.items();
    }

    public List<ActivityBlock> blocks() {
        return content.blocks();
    }

    public Activity activityOf(ActivityId activityId) {
        return content.activityOf(activityId);
    }

    public Set<ActivityId> predecessorsOf(ActivityId activityId) {
        return content.predecessorsOf(activityId);
    }

    public Duration duration(Function<Activity, Duration> leafDuration) {
        return content.duration(leafDuration);
    }

    public Duration estimatedDuration() {
        return content.estimatedDuration();
    }

    public RiskLevel estimatedRisk() {
        return content.estimatedRisk();
    }

    public Map<ConsumableId, Stock> estimatedConsumption() {
        return content.estimatedConsumption();
    }

    public Assignments assignments() {
        return content.assignments();
    }

    public List<PermitId> permits() {
        return content.permits();
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
