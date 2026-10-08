package edu.itba.fieldops.frameworks;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

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
    static final ArchRule apiDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.api..")
            .should().dependOnClassesThat().resideInAPackage("edu.itba.fieldops.adapters..");
}
