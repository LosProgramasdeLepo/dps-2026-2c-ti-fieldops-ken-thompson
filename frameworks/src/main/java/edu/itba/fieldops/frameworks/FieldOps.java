package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.expedition.AssignmentSuggester;
import edu.itba.fieldops.domain.expedition.Replanner;
import edu.itba.fieldops.domain.validation.RuleBasedValidator;
import edu.itba.fieldops.usecase.catalog.AdministerEquipment;
import edu.itba.fieldops.usecase.catalog.AdministerEquipmentInteractor;
import edu.itba.fieldops.usecase.catalog.AdministerPermits;
import edu.itba.fieldops.usecase.catalog.AdministerPermitsInteractor;
import edu.itba.fieldops.usecase.catalog.AdministerPersonnel;
import edu.itba.fieldops.usecase.catalog.AdministerPersonnelInteractor;
import edu.itba.fieldops.usecase.catalog.ConsultEquipment;
import edu.itba.fieldops.usecase.catalog.ConsultEquipmentInteractor;
import edu.itba.fieldops.usecase.catalog.ConsultPermits;
import edu.itba.fieldops.usecase.catalog.ConsultPermitsInteractor;
import edu.itba.fieldops.usecase.catalog.ConsultPersonnel;
import edu.itba.fieldops.usecase.catalog.ConsultPersonnelInteractor;
import edu.itba.fieldops.usecase.expedition.ApproveExpedition;
import edu.itba.fieldops.usecase.expedition.ApproveExpeditionInteractor;
import edu.itba.fieldops.usecase.expedition.AssignResources;
import edu.itba.fieldops.usecase.expedition.AssignResourcesInteractor;
import edu.itba.fieldops.usecase.expedition.ConsultExpedition;
import edu.itba.fieldops.usecase.expedition.ConsultExpeditionInteractor;
import edu.itba.fieldops.usecase.expedition.DraftExpedition;
import edu.itba.fieldops.usecase.expedition.DraftExpeditionInteractor;
import edu.itba.fieldops.usecase.expedition.ExecutionRepository;
import edu.itba.fieldops.usecase.expedition.ExpeditionRepository;
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
import edu.itba.fieldops.usecase.shared.Clock;

import java.util.Objects;

public final class FieldOps {
    private final AdministerPersonnel personnel;
    private final AdministerEquipment equipment;
    private final AdministerPermits permitting;
    private final ConsultPersonnel consultPersonnel;
    private final ConsultEquipment consultEquipment;
    private final ConsultPermits consultPermits;
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

    public FieldOps(FieldOpsRepositories repositories, Clock clock) {
        Objects.requireNonNull(repositories, "repositories");
        Objects.requireNonNull(clock, "clock");
        RuleBasedValidator validator = RuleBasedValidator.withDefaultRules();
        Replanner replanner = new Replanner(new AssignmentSuggester());
        Catalogs catalogs = repositories.catalogs();
        ExpeditionRepository plans = repositories.plans();
        ExecutionRepository runs = repositories.runs();
        personnel = new AdministerPersonnelInteractor(repositories.certifications(), repositories.people());
        equipment = new AdministerEquipmentInteractor(
                repositories.vehicles(), repositories.instruments(), repositories.consumables()
        );
        permitting = new AdministerPermitsInteractor(repositories.permits());
        consultPersonnel = new ConsultPersonnelInteractor(repositories.certifications(), repositories.people());
        consultEquipment = new ConsultEquipmentInteractor(
                repositories.vehicles(), repositories.instruments(), repositories.consumables()
        );
        consultPermits = new ConsultPermitsInteractor(repositories.permits());
        drafts = new DraftExpeditionInteractor(plans, repositories.people());
        itinerary = new PlanItineraryInteractor(plans);
        estimates = new EstimateExpeditionInteractor(plans);
        assignments = new AssignResourcesInteractor(plans, runs, catalogs, new AssignmentSuggester());
        review = new ReviewExpeditionInteractor(plans, runs, catalogs, validator);
        approval = new ApproveExpeditionInteractor(plans, runs, catalogs, validator);
        consult = new ConsultExpeditionInteractor(plans);
        tracking = new TrackExpeditionInteractor(plans, runs, clock);
        incidents = new RecordIncidentInteractor(plans, runs, clock, catalogs, replanner, repositories.proposals());
        proposalReview = new ReviewReplanProposalInteractor(plans, repositories.proposals(), clock);
        replan = new ReplanExpeditionInteractor(plans, runs, catalogs, replanner);
        reports = new ReportExpeditionInteractor(plans, runs);
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

    public ConsultPersonnel consultPersonnel() {
        return consultPersonnel;
    }

    public ConsultEquipment consultEquipment() {
        return consultEquipment;
    }

    public ConsultPermits consultPermits() {
        return consultPermits;
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
