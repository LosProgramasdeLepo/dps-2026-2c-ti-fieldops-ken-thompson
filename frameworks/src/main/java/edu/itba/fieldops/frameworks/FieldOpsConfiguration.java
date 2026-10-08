package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.SystemClock;
import edu.itba.fieldops.usecase.catalog.AdministerEquipment;
import edu.itba.fieldops.usecase.catalog.AdministerPermits;
import edu.itba.fieldops.usecase.catalog.AdministerPersonnel;
import edu.itba.fieldops.usecase.catalog.ConsultEquipment;
import edu.itba.fieldops.usecase.catalog.ConsultPermits;
import edu.itba.fieldops.usecase.catalog.ConsultPersonnel;
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
import edu.itba.fieldops.usecase.shared.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FieldOpsConfiguration {
    @Bean
    Clock systemClock() {
        return new SystemClock();
    }

    @Bean
    FieldOpsRepositories repositories(Clock clock) {
        return new InMemoryFieldOps(clock).repositories();
    }

    @Bean
    FieldOps fieldOps(FieldOpsRepositories repositories, Clock clock) {
        return new FieldOps(repositories, clock);
    }

    @Bean
    AdministerPersonnel personnel(FieldOps fieldOps) {
        return fieldOps.personnel();
    }

    @Bean
    AdministerEquipment equipment(FieldOps fieldOps) {
        return fieldOps.equipment();
    }

    @Bean
    AdministerPermits permitting(FieldOps fieldOps) {
        return fieldOps.permitting();
    }

    @Bean
    ConsultPersonnel consultPersonnel(FieldOps fieldOps) {
        return fieldOps.consultPersonnel();
    }

    @Bean
    ConsultEquipment consultEquipment(FieldOps fieldOps) {
        return fieldOps.consultEquipment();
    }

    @Bean
    ConsultPermits consultPermits(FieldOps fieldOps) {
        return fieldOps.consultPermits();
    }

    @Bean
    DraftExpedition drafts(FieldOps fieldOps) {
        return fieldOps.drafts();
    }

    @Bean
    PlanItinerary itinerary(FieldOps fieldOps) {
        return fieldOps.itinerary();
    }

    @Bean
    EstimateExpedition estimates(FieldOps fieldOps) {
        return fieldOps.estimates();
    }

    @Bean
    AssignResources assignments(FieldOps fieldOps) {
        return fieldOps.assignments();
    }

    @Bean
    ReviewExpedition review(FieldOps fieldOps) {
        return fieldOps.review();
    }

    @Bean
    ApproveExpedition approval(FieldOps fieldOps) {
        return fieldOps.approval();
    }

    @Bean
    ConsultExpedition consult(FieldOps fieldOps) {
        return fieldOps.consult();
    }

    @Bean
    TrackExpedition tracking(FieldOps fieldOps) {
        return fieldOps.tracking();
    }

    @Bean
    RecordIncident incidents(FieldOps fieldOps) {
        return fieldOps.incidents();
    }

    @Bean
    ReviewReplanProposal proposalReview(FieldOps fieldOps) {
        return fieldOps.proposalReview();
    }

    @Bean
    ReplanExpedition replan(FieldOps fieldOps) {
        return fieldOps.replan();
    }

    @Bean
    ReportExpedition reports(FieldOps fieldOps) {
        return fieldOps.reports();
    }
}
