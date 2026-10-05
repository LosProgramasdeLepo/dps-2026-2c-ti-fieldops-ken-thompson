package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.FixedClock;
import edu.itba.fieldops.adapters.InMemoryExecutionRepository;
import edu.itba.fieldops.adapters.InMemoryExpeditionRepository;
import edu.itba.fieldops.adapters.InMemoryReplanProposalRepository;
import edu.itba.fieldops.adapters.ResourceCatalog;
import edu.itba.fieldops.domain.expedition.AssignmentSuggester;
import edu.itba.fieldops.domain.expedition.Replanner;
import edu.itba.fieldops.domain.validation.RuleBasedValidator;
import edu.itba.fieldops.usecase.catalog.AdministerEquipment;
import edu.itba.fieldops.usecase.catalog.AdministerEquipmentInteractor;
import edu.itba.fieldops.usecase.catalog.AdministerPermits;
import edu.itba.fieldops.usecase.catalog.AdministerPermitsInteractor;
import edu.itba.fieldops.usecase.catalog.AdministerPersonnel;
import edu.itba.fieldops.usecase.catalog.AdministerPersonnelInteractor;
import edu.itba.fieldops.usecase.expedition.ApproveExpedition;
import edu.itba.fieldops.usecase.expedition.ApproveExpeditionInteractor;
import edu.itba.fieldops.usecase.expedition.AssignResources;
import edu.itba.fieldops.usecase.expedition.AssignResourcesInteractor;
import edu.itba.fieldops.usecase.expedition.ConsultExpedition;
import edu.itba.fieldops.usecase.expedition.ConsultExpeditionInteractor;
import edu.itba.fieldops.usecase.expedition.DraftExpedition;
import edu.itba.fieldops.usecase.expedition.DraftExpeditionInteractor;
import edu.itba.fieldops.usecase.expedition.PlanItinerary;
import edu.itba.fieldops.usecase.expedition.PlanItineraryInteractor;
import edu.itba.fieldops.usecase.expedition.RecordIncident;
import edu.itba.fieldops.usecase.expedition.RecordIncidentInteractor;
import edu.itba.fieldops.usecase.expedition.ReplanExpedition;
import edu.itba.fieldops.usecase.expedition.ReplanExpeditionInteractor;
import edu.itba.fieldops.usecase.expedition.ReviewExpedition;
import edu.itba.fieldops.usecase.expedition.ReviewExpeditionInteractor;
import edu.itba.fieldops.usecase.expedition.ReviewReplanProposal;
import edu.itba.fieldops.usecase.expedition.ReviewReplanProposalInteractor;
import edu.itba.fieldops.usecase.expedition.TrackExpedition;
import edu.itba.fieldops.usecase.expedition.TrackExpeditionInteractor;
import edu.itba.fieldops.usecase.report.EstimateExpedition;
import edu.itba.fieldops.usecase.report.EstimateExpeditionInteractor;
import edu.itba.fieldops.usecase.report.ReportExpedition;
import edu.itba.fieldops.usecase.report.ReportExpeditionInteractor;

import java.time.Instant;
import java.util.Objects;

public final class InMemoryFieldOps {
    private final ResourceCatalog catalog = new ResourceCatalog();
    private final InMemoryExpeditionRepository plans = new InMemoryExpeditionRepository();
    private final InMemoryExecutionRepository runs = new InMemoryExecutionRepository();
    private final InMemoryReplanProposalRepository proposals = new InMemoryReplanProposalRepository();
    private final FixedClock clock;

    private final AdministerPersonnel personnel;
    private final AdministerEquipment equipment;
    private final AdministerPermits permitting;
    private final DraftExpedition drafts;
    private final PlanItinerary itinerary;
    private final EstimateExpedition estimates;
    private final AssignResources assignments;
    private final ReviewExpedition review;
    private final ApproveExpedition approval;
    private final ConsultExpedition consult;
    private final TrackExpedition tracking;
    private final RecordIncident incidents;
    private final ReviewReplanProposal proposalReview;
    private final ReplanExpedition replan;
    private final ReportExpedition reports;

    public InMemoryFieldOps(Instant now) {
        this.clock = new FixedClock(Objects.requireNonNull(now, "now"));
        RuleBasedValidator validator = RuleBasedValidator.withDefaultRules();
        Replanner replanner = new Replanner(new AssignmentSuggester());
        personnel = new AdministerPersonnelInteractor(catalog, catalog);
        equipment = new AdministerEquipmentInteractor(catalog, catalog, catalog);
        permitting = new AdministerPermitsInteractor(catalog);
        drafts = new DraftExpeditionInteractor(plans, catalog);
        itinerary = new PlanItineraryInteractor(plans);
        estimates = new EstimateExpeditionInteractor(plans);
        assignments = new AssignResourcesInteractor(plans, runs, catalog.catalogs(), new AssignmentSuggester());
        review = new ReviewExpeditionInteractor(plans, runs, catalog.catalogs(), validator);
        approval = new ApproveExpeditionInteractor(plans, runs, catalog.catalogs(), validator);
        consult = new ConsultExpeditionInteractor(plans);
        tracking = new TrackExpeditionInteractor(plans, runs, clock);
        incidents = new RecordIncidentInteractor(plans, runs, clock, catalog.catalogs(), replanner, proposals);
        proposalReview = new ReviewReplanProposalInteractor(plans, proposals, clock);
        replan = new ReplanExpeditionInteractor(plans, runs, catalog.catalogs(), replanner);
        reports = new ReportExpeditionInteractor(plans, runs);
    }

    public ResourceCatalog catalog() {
        return catalog;
    }

    public InMemoryExpeditionRepository plans() {
        return plans;
    }

    public InMemoryExecutionRepository runs() {
        return runs;
    }

    public InMemoryReplanProposalRepository proposals() {
        return proposals;
    }

    public FixedClock clock() {
        return clock;
    }

    public AdministerPersonnel personnel() {
        return personnel;
    }

    public AdministerEquipment equipment() {
        return equipment;
    }

    public AdministerPermits permitting() {
        return permitting;
    }

    public DraftExpedition drafts() {
        return drafts;
    }

    public PlanItinerary itinerary() {
        return itinerary;
    }

    public EstimateExpedition estimates() {
        return estimates;
    }

    public AssignResources assignments() {
        return assignments;
    }

    public ReviewExpedition review() {
        return review;
    }

    public ApproveExpedition approval() {
        return approval;
    }

    public ConsultExpedition consult() {
        return consult;
    }

    public TrackExpedition tracking() {
        return tracking;
    }

    public RecordIncident incidents() {
        return incidents;
    }

    public ReviewReplanProposal proposalReview() {
        return proposalReview;
    }

    public ReplanExpedition replan() {
        return replan;
    }

    public ReportExpedition reports() {
        return reports;
    }
}
