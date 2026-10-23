package edu.itba.fieldops.frameworks;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import edu.itba.fieldops.domain.catalog.CertificationRegistry;
import edu.itba.fieldops.domain.catalog.ConsumableRegistry;
import edu.itba.fieldops.domain.catalog.InstrumentRegistry;
import edu.itba.fieldops.domain.catalog.PermitRegistry;
import edu.itba.fieldops.domain.catalog.PersonRegistry;
import edu.itba.fieldops.domain.catalog.VehicleRegistry;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.shared.Clock;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.itba.fieldops", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "edu.itba.fieldops.usecase..",
                    "edu.itba.fieldops.adapters..",
                    "edu.itba.fieldops.api..",
                    "edu.itba.fieldops.frameworks.."
            );

    @ArchTest
    static final ArchRule applicationDoesNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.usecase..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "edu.itba.fieldops.adapters..",
                    "edu.itba.fieldops.api..",
                    "edu.itba.fieldops.frameworks.."
            );

    @ArchTest
    static final ArchRule persistenceStaysOutOfTheDomainAndTheUseCases = noClasses()
            .that().resideInAnyPackage("edu.itba.fieldops.domain..", "edu.itba.fieldops.usecase..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "org.springframework.."
            );

    @ArchTest
    static final ArchRule theModelDoesNotCallUseCasesOrWritePorts = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.domain..")
            .and().resideOutsideOfPackage("edu.itba.fieldops.domain.usecase..")
            .should().dependOnClassesThat(
                    resideInAPackage("edu.itba.fieldops.domain.usecase..").or(belongToAnyOf(
                            ExpeditionRepository.class,
                            ExecutionRepository.class,
                            ReplanProposalRepository.class,
                            PersonRegistry.class,
                            VehicleRegistry.class,
                            InstrumentRegistry.class,
                            ConsumableRegistry.class,
                            PermitRegistry.class,
                            CertificationRegistry.class,
                            Clock.class
                    ))
            );

    @ArchTest
    static final ArchRule apiDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.api..")
            .should().dependOnClassesThat().resideInAPackage("edu.itba.fieldops.adapters..");
}
