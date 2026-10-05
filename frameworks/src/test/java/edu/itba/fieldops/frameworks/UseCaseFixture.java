package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.FixedClock;
import edu.itba.fieldops.adapters.InMemoryExecutionRepository;
import edu.itba.fieldops.adapters.InMemoryExpeditionRepository;
import edu.itba.fieldops.adapters.InMemoryReplanProposalRepository;
import edu.itba.fieldops.adapters.ResourceCatalog;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.usecase.catalog.AdministerEquipment;
import edu.itba.fieldops.usecase.catalog.AdministerPermits;
import edu.itba.fieldops.usecase.catalog.AdministerPersonnel;
import edu.itba.fieldops.usecase.expedition.ApproveExpedition;
import edu.itba.fieldops.usecase.expedition.AssignResources;
import edu.itba.fieldops.usecase.expedition.ConsultExpedition;
import edu.itba.fieldops.usecase.expedition.DraftExpedition;
import edu.itba.fieldops.usecase.expedition.PlanItinerary;
import edu.itba.fieldops.usecase.expedition.RecordIncident;
import edu.itba.fieldops.usecase.expedition.ReplanExpedition;
import edu.itba.fieldops.usecase.expedition.ReviewExpedition;
import edu.itba.fieldops.usecase.expedition.ReviewReplanProposal;
import edu.itba.fieldops.usecase.expedition.TrackExpedition;
import edu.itba.fieldops.usecase.report.EstimateExpedition;
import edu.itba.fieldops.usecase.report.ReportExpedition;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

abstract class UseCaseFixture {
    static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    static final WorkZone DELTA = new WorkZone("Delta");
    static final TimePeriod PERIOD = new TimePeriod(DAY, DAY.plus(Duration.ofDays(5)));

    final FixedClock clock = new FixedClock(DAY);
    private final InMemoryFieldOps app = new InMemoryFieldOps(clock);

    final ResourceCatalog catalog = app.catalog();
    final InMemoryExpeditionRepository plans = app.plans();
    final InMemoryExecutionRepository runs = app.runs();
    final InMemoryReplanProposalRepository proposals = app.proposals();
    final AdministerPersonnel personnel = app.personnel();
    final AdministerEquipment equipment = app.equipment();
    final AdministerPermits permitting = app.permitting();
    final DraftExpedition drafts = app.drafts();
    final PlanItinerary itinerary = app.itinerary();
    final EstimateExpedition estimates = app.estimates();
    final AssignResources assignments = app.assignments();
    final ReviewExpedition review = app.review();
    final ApproveExpedition approval = app.approval();
    final ConsultExpedition consult = app.consult();
    final TrackExpedition tracking = app.tracking();
    final RecordIncident incidents = app.incidents();
    final ReviewReplanProposal proposalReview = app.proposalReview();
    final ReplanExpedition replan = app.replan();
    final ReportExpedition reports = app.reports();

    ExpeditionId draftResponsibleFor(PersonId responsible) {
        return drafts.draft(new ExpeditionCharter(
                List.of(new Objective("Map wetland")),
                PERIOD,
                List.of(DELTA),
                List.of(responsible),
                List.of(new Restriction("Daylight only"))
        ));
    }

    CertificationId certification() {
        return personnel.registerCertification("Sampling");
    }

    PersonId certifiedPerson(String name, CertificationId certificationId) {
        return personnel.registerPerson(name, List.of(certificationId), Availability.always());
    }

    Sampling unassignedSampling(Map<ConsumableId, Stock> estimated) {
        CertificationId certificationId = certification();
        PersonId ada = certifiedPerson("Ada", certificationId);
        ExpeditionId expeditionId = draftResponsibleFor(ada);
        ActivityId activityId = itinerary.nextActivityId();
        itinerary.addActivity(expeditionId, Activity.sampling(certificationId)
                .named(activityId, "Soil sampling")
                .estimated(Duration.ofHours(4), RiskLevel.MEDIUM)
                .in(DELTA, hours(0, 4))
                .consuming(estimated)
                .build());
        return new Sampling(expeditionId, activityId, ada);
    }

    Sampling samplingPlan() {
        return samplingPlan(Map.of());
    }

    Sampling samplingPlan(Map<ConsumableId, Stock> estimated) {
        Sampling sampling = unassignedSampling(estimated);
        assignments.addAssignment(sampling.expeditionId(), new PersonAssignment(sampling.activityId(), sampling.responsible()));
        assignments.addPermit(sampling.expeditionId(), permitting.registerPermit(PermitKind.ZONE, DELTA, PERIOD));
        return sampling;
    }

    Sampling approvedSampling() {
        Sampling sampling = samplingPlan();
        review.submit(sampling.expeditionId());
        approval.approve(sampling.expeditionId());
        return sampling;
    }

    Sampling runningSampling() {
        Sampling sampling = approvedSampling();
        tracking.start(sampling.expeditionId());
        return sampling;
    }

    TwoSamplings approvedTwoSamplings() {
        TwoSamplings plan = twoSamplings();
        review.submit(plan.expeditionId());
        approval.approve(plan.expeditionId());
        return plan;
    }

    TwoSamplings twoSamplings() {
        Sampling sampling = samplingPlan();
        CertificationId certificationId = certification();
        PersonId bob = certifiedPerson("Bob", certificationId);
        ActivityId later = itinerary.nextActivityId();
        itinerary.addActivity(sampling.expeditionId(), sampling(certificationId, later, hours(4, 6)));
        assignments.addAssignment(sampling.expeditionId(), new PersonAssignment(later, bob));
        return new TwoSamplings(sampling.expeditionId(), sampling.activityId(), later, sampling.responsible());
    }

    ExpeditionId revisionWithout(ExpeditionId approvedId, ActivityId activityId) {
        ExpeditionId revision = replan.revise(approvedId);
        replan.cancel(revision, activityId);
        return revision;
    }

    Crossing crossing(PersonId responsible, VehicleId vehicle) {
        ExpeditionId expeditionId = draftResponsibleFor(responsible);
        ActivityId activityId = itinerary.nextActivityId();
        itinerary.addActivity(expeditionId, Activity.transit()
                .named(activityId, "Crossing")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, hours(0, 2))
                .build());
        assignments.addAssignment(expeditionId, new VehicleAssignment(activityId, vehicle));
        assignments.addPermit(expeditionId, permitting.registerPermit(PermitKind.ZONE, DELTA, PERIOD));
        return new Crossing(expeditionId, activityId);
    }

    ExpeditionId crowdedCrossingInReview() {
        PersonId ada = personnel.registerPerson("Ada", List.of(), Availability.always());
        PersonId bob = personnel.registerPerson("Bob", List.of(), Availability.always());
        Crossing crossing = crossing(ada, equipment.registerVehicle(new Passengers(1), Availability.always()));
        assignments.addAssignment(crossing.expeditionId(), new PersonAssignment(crossing.activityId(), ada));
        assignments.addAssignment(crossing.expeditionId(), new PersonAssignment(crossing.activityId(), bob));
        review.submit(crossing.expeditionId());
        return crossing.expeditionId();
    }

    static Activity sampling(CertificationId certificationId, ActivityId activityId, TimePeriod window) {
        return Activity.sampling(certificationId)
                .named(activityId, "Sampling")
                .estimated(Duration.between(window.start(), window.end()), RiskLevel.LOW)
                .in(DELTA, window)
                .build();
    }

    static TimePeriod hours(int fromHour, int toHour) {
        return new TimePeriod(DAY.plus(Duration.ofHours(fromHour)), DAY.plus(Duration.ofHours(toHour)));
    }

    static Instant at(int hour) {
        return DAY.plus(Duration.ofHours(hour));
    }

    record Sampling(ExpeditionId expeditionId, ActivityId activityId, PersonId responsible) {
    }

    record TwoSamplings(ExpeditionId expeditionId, ActivityId first, ActivityId later, PersonId firstPerson) {
    }

    record Crossing(ExpeditionId expeditionId, ActivityId activityId) {
    }
}
