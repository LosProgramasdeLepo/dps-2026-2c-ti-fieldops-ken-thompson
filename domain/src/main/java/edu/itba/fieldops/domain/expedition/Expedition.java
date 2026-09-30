package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.itinerary.Itinerary;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Expedition {
    private final ExpeditionId id;
    private final int version;
    private final ExpeditionId supersedes;
    private final List<Objective> objectives;
    private final TimePeriod period;
    private final List<WorkZone> zones;
    private final List<PersonId> responsibles;
    private final List<Restriction> restrictions;
    private final Itinerary itinerary;
    private final Assignments assignments;
    private final List<PermitId> permits;
    private final AcceptedWarnings acceptedWarnings;
    private ExpeditionStatus status;

    static Expedition draft(
            ExpeditionId id,
            List<Objective> objectives,
            TimePeriod period,
            List<WorkZone> zones,
            List<PersonId> responsibles,
            List<Restriction> restrictions
    ) {
        return new Expedition(id, objectives, period, zones, responsibles, restrictions);
    }

    private Expedition(
            ExpeditionId id,
            List<Objective> objectives,
            TimePeriod period,
            List<WorkZone> zones,
            List<PersonId> responsibles,
            List<Restriction> restrictions
    ) {
        this.id = Objects.requireNonNull(id, "expedition id");
        this.version = 1;
        this.supersedes = null;
        this.objectives = copyRequired(objectives, "objectives");
        this.period = Objects.requireNonNull(period, "period");
        this.zones = copyRequired(zones, "zones");
        this.responsibles = copyRequired(responsibles, "responsibles");
        this.restrictions = List.copyOf(restrictions);
        this.itinerary = new Itinerary();
        this.assignments = new Assignments();
        this.permits = new ArrayList<>();
        this.acceptedWarnings = new AcceptedWarnings();
        this.status = ExpeditionStatus.DRAFT;
    }

    private Expedition(Expedition source) {
        this.id = new ExpeditionId(UUID.randomUUID());
        this.version = source.version + 1;
        this.supersedes = source.id;
        this.objectives = source.objectives;
        this.period = source.period;
        this.zones = source.zones;
        this.responsibles = source.responsibles;
        this.restrictions = source.restrictions;
        this.itinerary = source.itinerary.copy();
        this.assignments = source.assignments.copy();
        this.permits = new ArrayList<>(source.permits);
        this.acceptedWarnings = new AcceptedWarnings();
        this.status = ExpeditionStatus.DRAFT;
    }

    Expedition reviseAsDraft() {
        return new Expedition(this);
    }

    void addActivity(Activity activity) {
        requireStatus(ExpeditionStatus.DRAFT, "add activity");
        Objects.requireNonNull(activity, "activity");
        if (!zones.contains(activity.zone())) {
            throw new InvalidItinerary("activity zone is not part of the expedition");
        }
        requireWindowInsidePeriod(activity);
        itinerary.add(activity);
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
        for (Activity activity : itinerary.delayed(activityId, delay)) {
            requireWindowInsidePeriod(activity);
        }
        itinerary.delay(activityId, delay);
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
        if (!responsibles.contains(warning.acceptedBy())) {
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

    public boolean hasAccepted(ValidationIssue warning) {
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

    public List<Objective> objectives() {
        return objectives;
    }

    public TimePeriod period() {
        return period;
    }

    public List<WorkZone> zones() {
        return zones;
    }

    public List<PersonId> responsibles() {
        return responsibles;
    }

    public List<Restriction> restrictions() {
        return restrictions;
    }

    public ExpeditionStatus status() {
        return status;
    }

    public List<Activity> itinerary() {
        return itinerary.activities();
    }

    public Assignments assignments() {
        return assignments;
    }

    public List<PermitId> permits() {
        return List.copyOf(permits);
    }

    public List<AcceptedWarning> acceptedWarnings() {
        return acceptedWarnings.all();
    }

    public Activity activityOf(ActivityId activityId) {
        return itinerary.activityOf(activityId);
    }

    private void requireWindowInsidePeriod(Activity activity) {
        if (!period.contains(activity.window())) {
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

    private static <T> List<T> copyRequired(List<T> values, String name) {
        if (values == null || values.isEmpty()) {
            throw new InvalidValue(name + " must not be empty");
        }
        return List.copyOf(values);
    }
}
