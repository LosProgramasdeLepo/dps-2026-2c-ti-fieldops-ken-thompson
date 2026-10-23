package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.catalog.Permits;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.Delay;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.Occurrence;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;

import java.time.Duration;
import java.time.Instant;
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

    public static Expedition draft(ExpeditionId id, ExpeditionCharter charter, People people) {
        Objects.requireNonNull(charter, "charter");
        Objects.requireNonNull(people, "people");
        for (PersonId responsible : charter.responsibles()) {
            if (people.person(responsible).isEmpty()) {
                throw new UnknownResource("responsible", responsible.value().toString());
            }
        }
        return new Expedition(id, charter);
    }

    public static Expedition restore(ExpeditionState state) {
        return new Expedition(Objects.requireNonNull(state, "expedition state"));
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

    private Expedition(ExpeditionState state) {
        this.id = state.id();
        this.version = state.version();
        this.supersedes = state.supersedes().orElse(null);
        this.content = PlanContent.restore(state);
        this.status = state.status();
        state.acceptedWarnings().forEach(this::register);
        requireRestorable();
    }

    public Expedition reviseAsDraft(ExpeditionId revisionId) {
        requireStatus(ExpeditionStatus.APPROVED, "revise");
        if (id.equals(revisionId)) {
            throw new InvalidValue("a revision needs its own id");
        }
        return new Expedition(revisionId, this);
    }

    public void addActivity(Activity activity) {
        requireStatus(ExpeditionStatus.DRAFT, "add activity");
        content.addActivity(activity);
    }

    public void addBlock(ActivityBlock block) {
        requireStatus(ExpeditionStatus.DRAFT, "add block");
        content.addBlock(block);
    }

    public void removeActivity(ActivityId activityId) {
        requireStatus(ExpeditionStatus.DRAFT, "remove activity");
        content.removeActivity(activityId);
    }

    public void addDependency(ActivityId activityId, ActivityId predecessorId) {
        requireStatus(ExpeditionStatus.DRAFT, "add dependency");
        content.addDependency(activityId, predecessorId);
    }

    public void addAssignment(Assignment assignment, Catalogs catalogs) {
        requireStatus(ExpeditionStatus.DRAFT, "assign resources");
        content.addAssignment(assignment, catalogs);
    }

    public void removeAssignment(Assignment assignment) {
        requireStatus(ExpeditionStatus.DRAFT, "unassign resources");
        content.removeAssignment(assignment);
    }

    public void delay(ActivityId activityId, Delay delay) {
        requireStatus(ExpeditionStatus.DRAFT, "delay activity");
        content.delay(activityId, delay);
    }

    public boolean delayFits(ActivityId activityId, Delay delay) {
        return content.delayFits(activityId, delay);
    }

    public void addPermit(PermitId permitId, Permits permits) {
        requireStatus(ExpeditionStatus.DRAFT, "attach permit");
        content.addPermit(permitId, permits);
    }

    ExpeditionExecution start(Instant at) {
        if (status != ExpeditionStatus.APPROVED) {
            throw new InvalidExpeditionTransition(status, "start");
        }
        requireWithinPeriod(at, "start");
        return ExpeditionExecution.begin(id);
    }

    ActivityStart startOf(ActivityId activityId, Instant at, List<Expedition> plans) {
        Expedition current = Revisions.inForce(this, Objects.requireNonNull(plans, "plans"));
        Activity activity = current.activityOf(activityId);
        if (!activity.window().contains(Objects.requireNonNull(at, "activity instant"))) {
            throw new InvalidActivityExecution("activity instant is outside the planned window");
        }
        return new ActivityStart(
                activityId,
                activity.window().at(at),
                current.predecessorsOf(activityId),
                Revisions.lineage(current, plans)
        );
    }

    Occurrence eventAt(Instant at, String event) {
        requireWithinPeriod(at, event);
        return charter().period().at(at);
    }

    public void acceptWarning(ExpeditionValidator validator, PlanningContexts contexts, AcceptedWarning warning) {
        requireStatus(ExpeditionStatus.IN_REVIEW, "accept warning");
        Objects.requireNonNull(warning, "warning");
        ValidationResult result = Objects.requireNonNull(validator, "validator").validate(contexts.around(this));
        if (!result.warnings().contains(warning.issue())) {
            throw new InvalidValue("not a current warning: " + warning.issue().code());
        }
        register(warning);
    }

    public void submitForReview(ExpeditionValidator validator, PlanningContexts contexts) {
        requireStatus(ExpeditionStatus.DRAFT, "submit for review");
        if (activities().isEmpty()) {
            throw new InvalidItinerary("expedition has no activities");
        }
        if (Objects.requireNonNull(validator, "validator").validate(contexts.around(this)).hasCritical()) {
            throw new ExpeditionNotApprovable("critical validation issues remain");
        }
        status = ExpeditionStatus.IN_REVIEW;
    }

    public void returnToDraft() {
        transition(ExpeditionStatus.IN_REVIEW, ExpeditionStatus.DRAFT, "return to draft");
        acceptedWarnings.clear();
    }

    public void approve(ExpeditionValidator validator, PlanningContexts contexts, Optional<Expedition> predecessor) {
        requireStatus(ExpeditionStatus.IN_REVIEW, "approve");
        if (!Objects.requireNonNull(predecessor, "predecessor").map(Expedition::id).equals(supersedes())) {
            throw new InvalidValue("approval must supersede the plan this one revises");
        }
        requireApprovable(Objects.requireNonNull(validator, "validator").validate(contexts.around(this)));
        predecessor.ifPresent(Expedition::markSuperseded);
        status = ExpeditionStatus.APPROVED;
    }

    void markSuperseded() {
        transition(ExpeditionStatus.APPROVED, ExpeditionStatus.SUPERSEDED, "supersede");
    }

    public boolean hasAccepted(ValidationIssue warning) {
        return acceptedWarnings.covers(warning);
    }

    public ExpeditionState state() {
        return new ExpeditionState(
                id,
                version,
                supersedes(),
                status,
                charter(),
                items(),
                assignments().all(),
                permits(),
                acceptedWarnings()
        );
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

    public boolean includes(ActivityId activityId) {
        return content.includes(activityId);
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

    public Map<ConsumableId, Consumption> estimatedConsumption() {
        return content.estimatedConsumption();
    }

    public Assignments assignments() {
        return content.assignments();
    }

    public List<PermitId> permits() {
        return content.permits();
    }

    private void requireApprovable(ValidationResult result) {
        if (result.hasCritical()) {
            throw new ExpeditionNotApprovable("critical validation issues remain");
        }
        for (ValidationIssue warning : result.warnings()) {
            if (!hasAccepted(warning)) {
                throw new ExpeditionNotApprovable("warning not justified: " + warning.code());
            }
        }
    }

    private void register(AcceptedWarning warning) {
        if (!charter().isResponsible(Objects.requireNonNull(warning, "warning").acceptedBy())) {
            throw new InvalidValue("warning must be accepted by a responsible");
        }
        acceptedWarnings.accept(warning);
    }

    private void requireRestorable() {
        if (status == ExpeditionStatus.DRAFT && !acceptedWarnings.all().isEmpty()) {
            throw new InvalidValue("a draft keeps no accepted warnings");
        }
        if (status != ExpeditionStatus.DRAFT && activities().isEmpty()) {
            throw new InvalidItinerary("expedition has no activities");
        }
    }

    private void transition(ExpeditionStatus from, ExpeditionStatus to, String action) {
        requireStatus(from, action);
        status = to;
    }

    private void requireWithinPeriod(Instant at, String event) {
        if (!charter().period().contains(Objects.requireNonNull(at, event))) {
            throw new InvalidActivityExecution(event + " is outside the expedition period");
        }
    }

    private void requireStatus(ExpeditionStatus expected, String action) {
        if (status != expected) {
            throw new InvalidExpeditionTransition(status, action);
        }
    }
}
