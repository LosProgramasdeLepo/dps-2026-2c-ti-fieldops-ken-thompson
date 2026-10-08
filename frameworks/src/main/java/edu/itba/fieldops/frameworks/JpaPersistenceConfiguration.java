package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.jpa.catalog.CertificationJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.ConsumableJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.InstrumentJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.JpaCertificationRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.JpaConsumableRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.JpaInstrumentRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.JpaPermitRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.JpaPersonRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.JpaVehicleRegistry;
import edu.itba.fieldops.adapters.jpa.catalog.PermitJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.PersonJpaRepository;
import edu.itba.fieldops.adapters.jpa.catalog.VehicleJpaRepository;
import edu.itba.fieldops.adapters.jpa.expedition.ExpeditionJpaRepository;
import edu.itba.fieldops.adapters.jpa.expedition.ExpeditionRegistrationJpaRepository;
import edu.itba.fieldops.adapters.jpa.expedition.JpaExpeditionRepository;
import edu.itba.fieldops.adapters.jpa.expedition.JpaReplanProposalRepository;
import edu.itba.fieldops.adapters.jpa.expedition.ProposalJpaRepository;
import edu.itba.fieldops.adapters.jpa.tracking.JpaExecutionRepository;
import edu.itba.fieldops.adapters.jpa.tracking.RunJpaRepository;
import edu.itba.fieldops.usecase.catalog.CertificationRegistry;
import edu.itba.fieldops.usecase.catalog.ConsumableRegistry;
import edu.itba.fieldops.usecase.catalog.InstrumentRegistry;
import edu.itba.fieldops.usecase.catalog.PermitRegistry;
import edu.itba.fieldops.usecase.catalog.PersonRegistry;
import edu.itba.fieldops.usecase.catalog.VehicleRegistry;
import edu.itba.fieldops.usecase.expedition.ExecutionRepository;
import edu.itba.fieldops.usecase.expedition.ExpeditionRepository;
import edu.itba.fieldops.usecase.expedition.ReplanProposalRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "fieldops.persistence", havingValue = "jpa", matchIfMissing = true)
@EnableJpaRepositories(basePackages = "edu.itba.fieldops.adapters.jpa")
@EntityScan(basePackages = "edu.itba.fieldops.adapters.jpa")
class JpaPersistenceConfiguration {
    @Bean
    TransactionOperations transactions(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    CertificationRegistry certifications(CertificationJpaRepository rows, TransactionOperations transactions) {
        return new JpaCertificationRegistry(rows, transactions);
    }

    @Bean
    PersonRegistry people(
            PersonJpaRepository rows,
            CertificationJpaRepository certifications,
            TransactionOperations transactions
    ) {
        return new JpaPersonRegistry(rows, certifications, transactions);
    }

    @Bean
    VehicleRegistry vehicles(VehicleJpaRepository rows, TransactionOperations transactions) {
        return new JpaVehicleRegistry(rows, transactions);
    }

    @Bean
    InstrumentRegistry instruments(InstrumentJpaRepository rows, TransactionOperations transactions) {
        return new JpaInstrumentRegistry(rows, transactions);
    }

    @Bean
    ConsumableRegistry consumables(ConsumableJpaRepository rows, TransactionOperations transactions) {
        return new JpaConsumableRegistry(rows, transactions);
    }

    @Bean
    PermitRegistry permits(PermitJpaRepository rows, TransactionOperations transactions) {
        return new JpaPermitRegistry(rows, transactions);
    }

    @Bean
    ExpeditionRepository plans(
            ExpeditionJpaRepository rows,
            ExpeditionRegistrationJpaRepository registrations,
            TransactionOperations transactions
    ) {
        return new JpaExpeditionRepository(rows, registrations, transactions);
    }

    @Bean
    ExecutionRepository runs(RunJpaRepository rows, TransactionOperations transactions) {
        return new JpaExecutionRepository(rows, transactions);
    }

    @Bean
    ReplanProposalRepository proposals(
            ProposalJpaRepository rows,
            ExpeditionJpaRepository expeditions,
            TransactionOperations transactions
    ) {
        return new JpaReplanProposalRepository(rows, expeditions, transactions);
    }

    @Bean
    FieldOpsRepositories repositories(
            CertificationRegistry certifications,
            PersonRegistry people,
            VehicleRegistry vehicles,
            InstrumentRegistry instruments,
            ConsumableRegistry consumables,
            PermitRegistry permits,
            ExpeditionRepository plans,
            ExecutionRepository runs,
            ReplanProposalRepository proposals
    ) {
        return new FieldOpsRepositories(
                certifications, people, vehicles, instruments, consumables, permits, plans, runs, proposals
        );
    }
}
