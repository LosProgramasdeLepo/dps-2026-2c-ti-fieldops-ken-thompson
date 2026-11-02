package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Capacity;
import edu.itba.fieldops.domain.catalog.CertificationName;
import edu.itba.fieldops.domain.catalog.ConsumableName;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.catalog.Stock;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.Justification;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.usecase.catalog.AdministerEquipment;
import edu.itba.fieldops.domain.usecase.catalog.AdministerPermits;
import edu.itba.fieldops.domain.usecase.catalog.AdministerPersonnel;
import edu.itba.fieldops.domain.usecase.expedition.ApproveExpedition;
import edu.itba.fieldops.domain.usecase.expedition.AssignResources;
import edu.itba.fieldops.domain.usecase.expedition.ConsultExpedition;
import edu.itba.fieldops.domain.usecase.expedition.DraftExpedition;
import edu.itba.fieldops.domain.usecase.expedition.PlanItinerary;
import edu.itba.fieldops.domain.usecase.expedition.RecordIncident;
import edu.itba.fieldops.domain.usecase.expedition.ReviewExpedition;
import edu.itba.fieldops.domain.usecase.expedition.TrackExpedition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Profile("demo")
@Transactional
public class DemoData implements ApplicationRunner {
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final InstrumentKind WATER_PROBE = new InstrumentKind("water probe");
    private static final InstrumentKind BINOCULARS = new InstrumentKind("binoculars");

    private final AdministerPersonnel personnel;
    private final AdministerEquipment equipment;
    private final AdministerPermits permitting;
    private final DraftExpedition drafts;
    private final PlanItinerary itinerary;
    private final AssignResources assignments;
    private final ReviewExpedition review;
    private final ApproveExpedition approval;
    private final TrackExpedition tracking;
    private final RecordIncident incidents;
    private final ConsultExpedition consult;
    private final Instant firstDay;
    private final TimePeriod period;

    public DemoData(
            AdministerPersonnel personnel,
            AdministerEquipment equipment,
            AdministerPermits permitting,
            DraftExpedition drafts,
            PlanItinerary itinerary,
            AssignResources assignments,
            ReviewExpedition review,
            ApproveExpedition approval,
            TrackExpedition tracking,
            RecordIncident incidents,
            ConsultExpedition consult,
            Clock clock
    ) {
        this.personnel = Objects.requireNonNull(personnel, "personnel");
        this.equipment = Objects.requireNonNull(equipment, "equipment");
        this.permitting = Objects.requireNonNull(permitting, "permitting");
        this.drafts = Objects.requireNonNull(drafts, "drafts");
        this.itinerary = Objects.requireNonNull(itinerary, "itinerary");
        this.assignments = Objects.requireNonNull(assignments, "assignments");
        this.review = Objects.requireNonNull(review, "review");
        this.approval = Objects.requireNonNull(approval, "approval");
        this.tracking = Objects.requireNonNull(tracking, "tracking");
        this.incidents = Objects.requireNonNull(incidents, "incidents");
        this.consult = Objects.requireNonNull(consult, "consult");
        Instant now = Objects.requireNonNull(clock, "clock").now().truncatedTo(ChronoUnit.HOURS);
        this.firstDay = now.plus(Duration.ofDays(1));
        this.period = new TimePeriod(now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(10)));
    }

    @Override
    public void run(ApplicationArguments arguments) {
        loadIfEmpty();
    }

    public void loadIfEmpty() {
        if (consult.all(new PageRequest(0, 1)).totalItems() == 0) {
            Catalog catalog = registerCatalog();
            planWetlandSurvey(catalog);
            planRidgeWatchWithoutNightPermit(catalog);
        }
    }

    private Catalog registerCatalog() {
        CertificationId sampling = personnel.registerCertification(new CertificationName("Field sampling"));
        CertificationId diving = personnel.registerCertification(new CertificationName("Scientific diving"));
        CertificationId night = personnel.registerCertification(new CertificationName("Night operation"));
        return new Catalog(
                sampling,
                diving,
                night,
                personnel.registerPerson(new PersonName("Ada Lovelace"), List.of(sampling, diving, night), Availability.always()),
                personnel.registerPerson(new PersonName("Bruno Diaz"), List.of(sampling, diving, night), Availability.always()),
                personnel.registerPerson(new PersonName("Carla Paz"), List.of(sampling), Availability.always()),
                personnel.registerPerson(new PersonName("Diego Sosa"), List.of(sampling), Availability.always()),
                equipment.registerVehicle(new Capacity(2), Availability.always()),
                equipment.registerVehicle(new Capacity(6), Availability.always()),
                equipment.registerInstrument(InstrumentKind.CAMP_GEAR, Availability.always()),
                equipment.registerInstrument(InstrumentKind.DIVING_GEAR, Availability.always()),
                equipment.registerInstrument(InstrumentKind.LIGHTING, Availability.always()),
                equipment.registerInstrument(WATER_PROBE, Availability.always()),
                equipment.registerInstrument(BINOCULARS, Availability.always()),
                equipment.registerConsumable(new ConsumableName("Fuel (litres)"), new Stock(200)),
                equipment.registerConsumable(new ConsumableName("Sample vials"), new Stock(100)),
                permitting.registerPermit(PermitKind.ZONE, DELTA, period),
                permitting.registerPermit(PermitKind.NIGHT, DELTA, period)
        );
    }

    private void planWetlandSurvey(Catalog catalog) {
        ExpeditionId survey = drafts.draft(new ExpeditionCharter(
                List.of(new Objective("Survey the biodiversity of the Delta wetlands"), new Objective("Measure water quality")),
                period,
                List.of(DELTA),
                List.of(catalog.ada(), catalog.bruno()),
                List.of(new Restriction("No motor boats inside the lagoon"))
        ));
        Activity drive = planned(Activity.transit().consuming(fuel(catalog, 20)), "Drive to base camp", new Slot(0, 2));
        Activity camp = planned(Activity.camp(), "Set up base camp", new Slot(2, 5));
        Activity eastSoil = planned(sampling(catalog, 10), "Soil sampling, east bank", new Slot(6, 10));
        Activity westSoil = planned(sampling(catalog, 10), "Soil sampling, west bank", new Slot(24, 28));
        Activity water = planned(Activity.measurement(catalog.sampling(), WATER_PROBE), "Water quality", new Slot(29, 32));
        Activity dive = planned(Activity.dive(catalog.diving()), "Lagoon dive", new Slot(48, 51));
        Activity census = planned(Activity.night(catalog.night()), "Night fauna census", new Slot(60, 63));
        Activity north = planned(Activity.transit().consuming(fuel(catalog, 15)), "Move to the north lagoon", new Slot(72, 74));
        Activity hides = planned(Activity.measurement(catalog.sampling(), BINOCULARS), "Install bird hides", new Slot(74, 76));
        Activity cores = planned(sampling(catalog, 5), "Sediment cores", new Slot(76, 80));
        Activity birds = planned(Activity.measurement(catalog.sampling(), BINOCULARS), "Bird count", new Slot(76, 79));
        Activity back = planned(Activity.transit(), "Return to base camp", new Slot(81, 83));
        List.of(drive, camp, eastSoil, westSoil, water, dive, census, north)
                .forEach(activity -> itinerary.addActivity(survey, activity));
        itinerary.addBlock(survey, ActivityBlock.sequential(hides, ActivityBlock.parallel(cores, birds), back));
        itinerary.addDependency(survey, camp.id(), drive.id());
        itinerary.addDependency(survey, eastSoil.id(), camp.id());
        itinerary.addDependency(survey, census.id(), dive.id());
        assign(survey, List.of(
                new PersonAssignment(drive.id(), catalog.ada()),
                new PersonAssignment(drive.id(), catalog.bruno()),
                new PersonAssignment(drive.id(), catalog.carla()),
                new VehicleAssignment(drive.id(), catalog.truck()),
                new ConsumableAssignment(drive.id(), catalog.fuel(), new Consumption(20)),
                new VehicleAssignment(camp.id(), catalog.van()),
                new InstrumentAssignment(camp.id(), catalog.campGear()),
                new PersonAssignment(eastSoil.id(), catalog.carla()),
                new ConsumableAssignment(eastSoil.id(), catalog.vials(), new Consumption(10)),
                new PersonAssignment(westSoil.id(), catalog.carla()),
                new ConsumableAssignment(westSoil.id(), catalog.vials(), new Consumption(10)),
                new PersonAssignment(water.id(), catalog.diego()),
                new InstrumentAssignment(water.id(), catalog.waterProbe()),
                new PersonAssignment(dive.id(), catalog.ada()),
                new PersonAssignment(dive.id(), catalog.bruno()),
                new InstrumentAssignment(dive.id(), catalog.divingGear()),
                new PersonAssignment(census.id(), catalog.ada()),
                new PersonAssignment(census.id(), catalog.bruno()),
                new InstrumentAssignment(census.id(), catalog.lamp()),
                new PersonAssignment(north.id(), catalog.carla()),
                new PersonAssignment(north.id(), catalog.diego()),
                new VehicleAssignment(north.id(), catalog.van()),
                new ConsumableAssignment(north.id(), catalog.fuel(), new Consumption(15)),
                new PersonAssignment(hides.id(), catalog.diego()),
                new InstrumentAssignment(hides.id(), catalog.binoculars()),
                new PersonAssignment(cores.id(), catalog.carla()),
                new ConsumableAssignment(cores.id(), catalog.vials(), new Consumption(5)),
                new PersonAssignment(birds.id(), catalog.diego()),
                new InstrumentAssignment(birds.id(), catalog.binoculars()),
                new PersonAssignment(back.id(), catalog.carla()),
                new PersonAssignment(back.id(), catalog.diego()),
                new VehicleAssignment(back.id(), catalog.van())
        ));
        assignments.addPermit(survey, catalog.zonePermit());
        assignments.addPermit(survey, catalog.nightPermit());
        approveAcceptingTheWarnings(survey, catalog.ada());
        tracking.start(survey);
        incidents.record(survey, new IncidentDescription("The access road to the north lagoon is flooded"), north.id());
    }

    private void planRidgeWatchWithoutNightPermit(Catalog catalog) {
        ExpeditionId ridge = drafts.draft(new ExpeditionCharter(
                List.of(new Objective("Watch nocturnal raptors on the ridge")),
                period,
                List.of(DELTA),
                List.of(catalog.bruno()),
                List.of()
        ));
        itinerary.addActivity(ridge, planned(Activity.night(catalog.night()), "Ridge night watch", new Slot(96, 99)));
        assignments.addPermit(ridge, catalog.zonePermit());
    }

    private void approveAcceptingTheWarnings(ExpeditionId expeditionId, PersonId responsible) {
        review.submit(expeditionId);
        review.validate(expeditionId).warnings().forEach(warning -> review.acceptWarning(
                expeditionId,
                new AcceptedWarning(warning, new Justification("A second vehicle follows the truck with the extra passenger"), responsible)
        ));
        approval.approve(expeditionId);
    }

    private void assign(ExpeditionId expeditionId, List<Assignment> planned) {
        planned.forEach(assignment -> assignments.addAssignment(expeditionId, assignment));
    }

    private Activity planned(Activity.Builder kind, String name, Slot slot) {
        return kind.named(itinerary.nextActivityId(), new ActivityName(name))
                .estimated(new EstimatedDuration(Duration.ofHours(slot.toHour() - slot.fromHour())), RiskLevel.MEDIUM)
                .in(DELTA, new TimePeriod(
                        firstDay.plus(Duration.ofHours(slot.fromHour())),
                        firstDay.plus(Duration.ofHours(slot.toHour()))
                ))
                .build();
    }

    private static Activity.Builder sampling(Catalog catalog, int vials) {
        return Activity.sampling(catalog.sampling()).consuming(Map.of(catalog.vials(), new Consumption(vials)));
    }

    private static Map<ConsumableId, Consumption> fuel(Catalog catalog, int litres) {
        return Map.of(catalog.fuel(), new Consumption(litres));
    }

    private record Slot(int fromHour, int toHour) {
    }

    private record Catalog(
            CertificationId sampling,
            CertificationId diving,
            CertificationId night,
            PersonId ada,
            PersonId bruno,
            PersonId carla,
            PersonId diego,
            VehicleId truck,
            VehicleId van,
            InstrumentId campGear,
            InstrumentId divingGear,
            InstrumentId lamp,
            InstrumentId waterProbe,
            InstrumentId binoculars,
            ConsumableId fuel,
            ConsumableId vials,
            PermitId zonePermit,
            PermitId nightPermit
    ) {
    }
}
