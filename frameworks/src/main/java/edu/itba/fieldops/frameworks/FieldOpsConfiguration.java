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
import org.springframework.transaction.support.TransactionOperations;

@Configuration
class FieldOpsConfiguration {
    @Bean
    Clock systemClock() {
        return new SystemClock();
    }

    @Bean
    FieldOps fieldOps(FieldOpsRepositories repositories, Clock clock) {
        return new FieldOps(repositories, clock);
    }

    @Bean
    AdministerPersonnel personnel(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(AdministerPersonnel.class, fieldOps.personnel(), transactions);
    }

    @Bean
    AdministerEquipment equipment(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(AdministerEquipment.class, fieldOps.equipment(), transactions);
    }

    @Bean
    AdministerPermits permitting(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(AdministerPermits.class, fieldOps.permitting(), transactions);
    }

    @Bean
    ConsultPersonnel consultPersonnel(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(ConsultPersonnel.class, fieldOps.consultPersonnel(), transactions);
    }

    @Bean
    ConsultEquipment consultEquipment(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(ConsultEquipment.class, fieldOps.consultEquipment(), transactions);
    }

    @Bean
    ConsultPermits consultPermits(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(ConsultPermits.class, fieldOps.consultPermits(), transactions);
    }

    @Bean
    DraftExpedition drafts(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(DraftExpedition.class, fieldOps.drafts(), transactions);
    }

    @Bean
    PlanItinerary itinerary(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(PlanItinerary.class, fieldOps.itinerary(), transactions);
    }

    @Bean
    EstimateExpedition estimates(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(EstimateExpedition.class, fieldOps.estimates(), transactions);
    }

    @Bean
    AssignResources assignments(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(AssignResources.class, fieldOps.assignments(), transactions);
    }

    @Bean
    ReviewExpedition review(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(ReviewExpedition.class, fieldOps.review(), transactions);
    }

    @Bean
    ApproveExpedition approval(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(ApproveExpedition.class, fieldOps.approval(), transactions);
    }

    @Bean
    ConsultExpedition consult(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(ConsultExpedition.class, fieldOps.consult(), transactions);
    }

    @Bean
    TrackExpedition tracking(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(TrackExpedition.class, fieldOps.tracking(), transactions);
    }

    @Bean
    RecordIncident incidents(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(RecordIncident.class, fieldOps.incidents(), transactions);
    }

    @Bean
    ReviewReplanProposal proposalReview(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(ReviewReplanProposal.class, fieldOps.proposalReview(), transactions);
    }

    @Bean
    ReplanExpedition replan(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(ReplanExpedition.class, fieldOps.replan(), transactions);
    }

    @Bean
    ReportExpedition reports(FieldOps fieldOps, TransactionOperations transactions) {
        return TransactionalUseCase.around(ReportExpedition.class, fieldOps.reports(), transactions);
    }
}
