package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Capacity;
import edu.itba.fieldops.domain.catalog.CertificationName;
import edu.itba.fieldops.domain.catalog.ConsumableRegistry;
import edu.itba.fieldops.domain.catalog.InstrumentRegistry;
import edu.itba.fieldops.domain.catalog.PermitRegistry;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.catalog.PersonRegistry;
import edu.itba.fieldops.domain.catalog.VehicleRegistry;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.Events;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.usecase.catalog.AdministerEquipment;
import edu.itba.fieldops.domain.usecase.catalog.AdministerPermits;
import edu.itba.fieldops.domain.usecase.catalog.AdministerPersonnel;
import edu.itba.fieldops.domain.usecase.catalog.ConsultEquipment;
import edu.itba.fieldops.domain.usecase.catalog.ConsultPermits;
import edu.itba.fieldops.domain.usecase.catalog.ConsultPersonnel;
import edu.itba.fieldops.domain.usecase.expedition.ApproveExpedition;
import edu.itba.fieldops.domain.usecase.expedition.AssignResources;
import edu.itba.fieldops.domain.usecase.expedition.ConsultExpedition;
import edu.itba.fieldops.domain.usecase.expedition.DraftExpedition;
import edu.itba.fieldops.domain.usecase.expedition.PlanItinerary;
import edu.itba.fieldops.domain.usecase.expedition.RecordIncident;
import edu.itba.fieldops.domain.usecase.expedition.ReplanExpedition;
import edu.itba.fieldops.domain.usecase.expedition.ReviewExpedition;
import edu.itba.fieldops.domain.usecase.expedition.ReviewReplanProposal;
import edu.itba.fieldops.domain.usecase.expedition.TrackExpedition;
import edu.itba.fieldops.domain.usecase.report.ReportExpedition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@ActiveProfiles(PersistenceProfiles.MEMORY)
@Import(UseCaseFixture.FixedTime.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
abstract class UseCaseFixture {
    static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    static final WorkZone DELTA = new WorkZone("Delta");
    static final TimePeriod PERIOD = new TimePeriod(DAY, DAY.plus(Duration.ofDays(5)));
    static final PageRequest FIRST_PAGE = new PageRequest(0, 20);

    @Autowired
    FixedClock clock;
    @Autowired
    PersonRegistry people;
    @Autowired
    VehicleRegistry vehicles;
    @Autowired
    InstrumentRegistry instruments;
    @Autowired
    ConsumableRegistry consumables;
    @Autowired
    PermitRegistry permits;
    @Autowired
    ExpeditionRepository plans;
    @Autowired
    ExecutionRepository runs;
    @Autowired
    Events eventLog;
    @Autowired
    AdministerPersonnel personnel;
    @Autowired
    AdministerEquipment equipment;
    @Autowired
    AdministerPermits permitting;
    @Autowired
    ConsultPersonnel consultPersonnel;
    @Autowired
    ConsultEquipment consultEquipment;
    @Autowired
    ConsultPermits consultPermits;
    @Autowired
    DraftExpedition drafts;
    @Autowired
    PlanItinerary itinerary;
    @Autowired
    AssignResources assignments;
    @Autowired
    ReviewExpedition review;
    @Autowired
    ApproveExpedition approval;
    @Autowired
    ConsultExpedition consult;
    @Autowired
    TrackExpedition tracking;
    @Autowired
    RecordIncident incidents;
    @Autowired
    ReviewReplanProposal proposalReview;
    @Autowired
    ReplanExpedition replan;
    @Autowired
    ReportExpedition reports;

    int eventMark() {
        return allEvents().size();
    }

    List<DomainEvent> eventsSince(int mark) {
        List<DomainEvent> all = allEvents();
        return all.subList(mark, all.size());
    }

    List<DomainEvent> allEvents() {
        return eventLog.page(EventFilter.all(), new PageRequest(0, PageRequest.MAX_SIZE)).items().stream()
                .map(RecordedEvent::event)
                .toList();
    }

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
        return personnel.registerCertification(new CertificationName("Sampling"));
    }

    PersonId certifiedPerson(String name, CertificationId certificationId) {
        return personnel.registerPerson(new PersonName(name), List.of(certificationId), Availability.always());
    }

    Sampling unassignedSampling() {
        CertificationId certificationId = certification();
        PersonId ada = certifiedPerson("Ada", certificationId);
        ExpeditionId expeditionId = draftResponsibleFor(ada);
        ActivityId activityId = itinerary.nextActivityId();
        itinerary.addActivity(expeditionId, Activity.sampling(certificationId)
                .named(activityId, new ActivityName("Soil sampling"))
                .estimated(new EstimatedDuration(Duration.ofHours(4)), RiskLevel.MEDIUM)
                .in(DELTA, hours(0, 4))
                .build());
        return new Sampling(expeditionId, activityId, ada);
    }

    Sampling samplingPlan() {
        Sampling sampling = unassignedSampling();
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
                .named(activityId, new ActivityName("Crossing"))
                .estimated(new EstimatedDuration(Duration.ofHours(2)), RiskLevel.LOW)
                .in(DELTA, hours(0, 2))
                .build());
        assignments.addAssignment(expeditionId, new VehicleAssignment(activityId, vehicle));
        assignments.addPermit(expeditionId, permitting.registerPermit(PermitKind.ZONE, DELTA, PERIOD));
        return new Crossing(expeditionId, activityId);
    }

    ExpeditionId crowdedCrossingInReview() {
        PersonId ada = personnel.registerPerson(new PersonName("Ada"), List.of(), Availability.always());
        PersonId bob = personnel.registerPerson(new PersonName("Bob"), List.of(), Availability.always());
        Crossing crossing = crossing(ada, equipment.registerVehicle(new Capacity(1), Availability.always()));
        assignments.addAssignment(crossing.expeditionId(), new PersonAssignment(crossing.activityId(), ada));
        assignments.addAssignment(crossing.expeditionId(), new PersonAssignment(crossing.activityId(), bob));
        review.submit(crossing.expeditionId());
        return crossing.expeditionId();
    }

    static Activity sampling(CertificationId certificationId, ActivityId activityId, TimePeriod window) {
        return Activity.sampling(certificationId)
                .named(activityId, new ActivityName("Sampling"))
                .estimated(new EstimatedDuration(Duration.between(window.start(), window.end())), RiskLevel.LOW)
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

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTime {
        @Bean
        @Primary
        FixedClock clock() {
            return new FixedClock(DAY);
        }
    }
}
