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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FieldOpsConfiguration {
    @Bean
    InMemoryFieldOps fieldOps() {
        return new InMemoryFieldOps(new SystemClock());
    }

    @Bean
    AdministerPersonnel personnel(InMemoryFieldOps fieldOps) {
        return fieldOps.personnel();
    }

    @Bean
    AdministerEquipment equipment(InMemoryFieldOps fieldOps) {
        return fieldOps.equipment();
    }

    @Bean
    AdministerPermits permitting(InMemoryFieldOps fieldOps) {
        return fieldOps.permitting();
    }

    @Bean
    ConsultPersonnel consultPersonnel(InMemoryFieldOps fieldOps) {
        return fieldOps.consultPersonnel();
    }

    @Bean
    ConsultEquipment consultEquipment(InMemoryFieldOps fieldOps) {
        return fieldOps.consultEquipment();
    }

    @Bean
    ConsultPermits consultPermits(InMemoryFieldOps fieldOps) {
        return fieldOps.consultPermits();
    }

    @Bean
    DraftExpedition drafts(InMemoryFieldOps fieldOps) {
        return fieldOps.drafts();
    }

    @Bean
    PlanItinerary itinerary(InMemoryFieldOps fieldOps) {
        return fieldOps.itinerary();
    }

    @Bean
    EstimateExpedition estimates(InMemoryFieldOps fieldOps) {
        return fieldOps.estimates();
    }

    @Bean
    AssignResources assignments(InMemoryFieldOps fieldOps) {
        return fieldOps.assignments();
    }

    @Bean
    ReviewExpedition review(InMemoryFieldOps fieldOps) {
        return fieldOps.review();
    }

    @Bean
    ApproveExpedition approval(InMemoryFieldOps fieldOps) {
        return fieldOps.approval();
    }

    @Bean
    ConsultExpedition consult(InMemoryFieldOps fieldOps) {
        return fieldOps.consult();
    }

    @Bean
    TrackExpedition tracking(InMemoryFieldOps fieldOps) {
        return fieldOps.tracking();
    }

    @Bean
    RecordIncident incidents(InMemoryFieldOps fieldOps) {
        return fieldOps.incidents();
    }

    @Bean
    ReviewReplanProposal proposalReview(InMemoryFieldOps fieldOps) {
        return fieldOps.proposalReview();
    }

    @Bean
    ReplanExpedition replan(InMemoryFieldOps fieldOps) {
        return fieldOps.replan();
    }

    @Bean
    ReportExpedition reports(InMemoryFieldOps fieldOps) {
        return fieldOps.reports();
    }
}
