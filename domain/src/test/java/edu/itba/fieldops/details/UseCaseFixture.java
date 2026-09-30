package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.catalog.AdministerCatalogInteractor;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.usecase.AdministerCatalog;
import edu.itba.fieldops.domain.expedition.ApproveExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.AssignResourcesInteractor;
import edu.itba.fieldops.domain.expedition.AssignmentSuggester;
import edu.itba.fieldops.domain.expedition.ConsultExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.DraftExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.PlanItineraryInteractor;
import edu.itba.fieldops.domain.expedition.RecordIncidentInteractor;
import edu.itba.fieldops.domain.expedition.ReplanExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.Replanner;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.expedition.ReviewExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.ReviewReplanProposalInteractor;
import edu.itba.fieldops.domain.expedition.TrackExpeditionInteractor;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.expedition.usecase.ApproveExpedition;
import edu.itba.fieldops.domain.expedition.usecase.AssignResources;
import edu.itba.fieldops.domain.expedition.usecase.ConsultExpedition;
import edu.itba.fieldops.domain.expedition.usecase.DraftExpedition;
import edu.itba.fieldops.domain.expedition.usecase.PlanItinerary;
import edu.itba.fieldops.domain.expedition.usecase.RecordIncident;
import edu.itba.fieldops.domain.expedition.usecase.ReplanExpedition;
import edu.itba.fieldops.domain.expedition.usecase.ReviewExpedition;
import edu.itba.fieldops.domain.expedition.usecase.ReviewReplanProposal;
import edu.itba.fieldops.domain.expedition.usecase.TrackExpedition;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.report.EstimateExpeditionInteractor;
import edu.itba.fieldops.domain.report.ReportExpeditionInteractor;
import edu.itba.fieldops.domain.report.usecase.EstimateExpedition;
import edu.itba.fieldops.domain.report.usecase.ReportExpedition;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.validation.RuleBasedValidator;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

abstract class UseCaseFixture {
    static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    static final WorkZone DELTA = new WorkZone("Delta");
    static final TimePeriod PERIOD = new TimePeriod(DAY, DAY.plus(Duration.ofDays(5)));

    final ResourceCatalog catalog = new ResourceCatalog();
    final InMemoryExpeditionRepository plans = new InMemoryExpeditionRepository();
    final InMemoryExecutionRepository runs = new InMemoryExecutionRepository();
    final InMemoryReplanProposalRepository proposals = new InMemoryReplanProposalRepository();
    final FixedClock clock = new FixedClock(DAY);

    private final RuleBasedValidator validator = RuleBasedValidator.withDefaultRules();
    private final Replanner replanner = new Replanner(new AssignmentSuggester());

    final AdministerCatalog registry = new AdministerCatalogInteractor(catalog, catalog.catalogs());
    final DraftExpedition drafts = new DraftExpeditionInteractor(plans, catalog);
    final PlanItinerary itinerary = new PlanItineraryInteractor(plans);
    final EstimateExpedition estimates = new EstimateExpeditionInteractor(plans);
    final AssignResources assignments = new AssignResourcesInteractor(plans, runs, catalog.catalogs(), new AssignmentSuggester());
    final ReviewExpedition review = new ReviewExpeditionInteractor(plans, runs, catalog.catalogs(), validator);
    final ApproveExpedition approval = new ApproveExpeditionInteractor(plans, runs, catalog.catalogs(), validator);
    final ConsultExpedition consult = new ConsultExpeditionInteractor(plans);
    final TrackExpedition tracking = new TrackExpeditionInteractor(plans, runs, clock);
    final RecordIncident incidents = new RecordIncidentInteractor(plans, runs, clock, catalog.catalogs(), replanner, proposals);
    final ReviewReplanProposal proposalReview = new ReviewReplanProposalInteractor(plans, proposals, clock);
    final ReplanExpedition replan = new ReplanExpeditionInteractor(plans, runs, catalog.catalogs(), replanner);
    final ReportExpedition reports = new ReportExpeditionInteractor(plans, runs);

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
        return new CertificationId(UUID.randomUUID());
    }

    PersonId certifiedPerson(String name, CertificationId certificationId) {
        return registry.registerPerson(name, List.of(new Certification(certificationId, "Sampling")), Availability.always());
    }

    Sampling unassignedSampling(Map<ConsumableId, Stock> estimated) {
        CertificationId certificationId = certification();
        PersonId ada = certifiedPerson("Ada", certificationId);
        ExpeditionId expeditionId = draftResponsibleFor(ada);
        ActivityId activityId = new ActivityId(UUID.randomUUID());
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
        assignments.addPermit(sampling.expeditionId(), registry.registerPermit(DELTA, PERIOD));
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
        ActivityId later = new ActivityId(UUID.randomUUID());
        itinerary.addActivity(sampling.expeditionId(), sampling(certificationId, later, hours(4, 6)));
        assignments.addAssignment(sampling.expeditionId(), new PersonAssignment(later, bob));
        return new TwoSamplings(sampling.expeditionId(), sampling.activityId(), later, sampling.responsible());
    }

    Crossing crossing(PersonId responsible, VehicleId vehicle) {
        ExpeditionId expeditionId = draftResponsibleFor(responsible);
        ActivityId activityId = new ActivityId(UUID.randomUUID());
        itinerary.addActivity(expeditionId, Activity.transit()
                .named(activityId, "Crossing")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, hours(0, 2))
                .build());
        assignments.addAssignment(expeditionId, new VehicleAssignment(activityId, vehicle));
        assignments.addPermit(expeditionId, registry.registerPermit(DELTA, PERIOD));
        return new Crossing(expeditionId, activityId);
    }

    ExpeditionId crowdedCrossingInReview() {
        PersonId ada = registry.registerPerson("Ada", List.of(), Availability.always());
        PersonId bob = registry.registerPerson("Bob", List.of(), Availability.always());
        Crossing crossing = crossing(ada, registry.registerVehicle(new Passengers(1), Availability.always()));
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
