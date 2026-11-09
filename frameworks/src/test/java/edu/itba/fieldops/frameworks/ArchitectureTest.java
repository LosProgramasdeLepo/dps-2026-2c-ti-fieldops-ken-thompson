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
import edu.itba.fieldops.domain.event.EventDeliveries;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionApprover;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionTracker;
import edu.itba.fieldops.domain.expedition.ExpeditionValidator;
import edu.itba.fieldops.domain.expedition.IncidentResponder;
import edu.itba.fieldops.domain.expedition.ProposalReviewer;
import edu.itba.fieldops.domain.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.notification.Notifier;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.usecase.audit.ConsultEventsInteractor;
import org.springframework.scheduling.annotation.Scheduled;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

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
    static final ArchRule theDomainIsFreeOfFrameworks = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta..",
                    "org.hibernate..",
                    "org.springframework.."
            );

    @ArchTest
    static final ArchRule persistenceStaysInTheAdapters = noClasses()
            .that().resideOutsideOfPackage("edu.itba.fieldops.adapters..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "org.springframework.data..",
                    "com.mongodb..",
                    "org.bson.."
            );

    @ArchTest
    static final ArchRule theWebStaysInTheApi = noClasses()
            .that().resideOutsideOfPackage("edu.itba.fieldops.api..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..",
                    "org.springframework.http..",
                    "jakarta.servlet..",
                    "jakarta.validation.."
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
                            EventLog.class,
                            EventDeliveries.class,
                            Notifier.class,
                            PersonRegistry.class,
                            VehicleRegistry.class,
                            InstrumentRegistry.class,
                            ConsumableRegistry.class,
                            PermitRegistry.class,
                            CertificationRegistry.class
                    ))
            );

    @ArchTest
    static final ArchRule onlyTheDomainServicesReadTheClockInTheDomain = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.domain..")
            .and().doNotBelongToAnyOf(
                    ExpeditionTracker.class,
                    ProposalReviewer.class,
                    ExpeditionApprover.class,
                    IncidentResponder.class
            )
            .should().dependOnClassesThat().belongToAnyOf(Clock.class);

    @ArchTest
    static final ArchRule onlyUseCasesPullTheEventsOfAnAggregate = noClasses()
            .that().resideOutsideOfPackage("edu.itba.fieldops.usecase..")
            .should().callMethodWhere(target(nameMatching("pullEvents"))
                    .and(target(owner(resideInAPackage("edu.itba.fieldops.domain..")))));

    @ArchTest
    static final ArchRule approvingAPlanIsNotPublic = methods()
            .that().areDeclaredIn(Expedition.class).and().haveName("approve")
            .should().notBePublic();

    @ArchTest
    static final ArchRule onlyTheApproverApprovesAPlan = noClasses()
            .that().doNotBelongToAnyOf(ExpeditionApprover.class, Expedition.class)
            .should().callMethodWhere(target(nameMatching("approve"))
                    .and(target(owner(belongToAnyOf(Expedition.class)))));

    @ArchTest
    static final ArchRule onlyTheAdaptersRestoreStoredState = noClasses()
            .that().resideInAnyPackage(
                    "edu.itba.fieldops.usecase..",
                    "edu.itba.fieldops.api..",
                    "edu.itba.fieldops.frameworks.."
            )
            .should().callMethodWhere(target(nameMatching("restore|restoring"))
                    .and(target(owner(resideInAPackage("edu.itba.fieldops.domain..")))));

    @ArchTest
    static final ArchRule theValidatorRunsTheDomainRules = classes()
            .that().implement(ExpeditionValidator.class)
            .should().resideInAPackage("edu.itba.fieldops.domain.validation..");

    @ArchTest
    static final ArchRule useCaseInterfacesAndPortsLiveInTheDomain = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.usecase..")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule apiDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.api..")
            .should().dependOnClassesThat().resideInAPackage("edu.itba.fieldops.adapters..");

    @ArchTest
    static final ArchRule onlyUseCasesAppendToTheEventLog = noClasses()
            .that().resideOutsideOfPackage("edu.itba.fieldops.usecase..")
            .should().callMethodWhere(target(nameMatching("append"))
                    .and(target(owner(assignableTo(EventLog.class)))));

    @ArchTest
    static final ArchRule consultingEventsOnlyReadsThem = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.usecase.audit..")
            .should().dependOnClassesThat().belongToAnyOf(EventLog.class);

    @ArchTest
    static final ArchRule consultingEventsDoesNotManageDeliveries = noClasses()
            .that().belongToAnyOf(ConsultEventsInteractor.class)
            .should().dependOnClassesThat().belongToAnyOf(EventDeliveries.class);

    @ArchTest
    static final ArchRule theConsumersOnlyReachTheDomainThroughUseCasesAndEvents = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.adapters.messaging..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "edu.itba.fieldops.adapters.jpa..",
                    "edu.itba.fieldops.adapters.mongo..",
                    "edu.itba.fieldops.domain.expedition..",
                    "edu.itba.fieldops.domain.catalog..",
                    "edu.itba.fieldops.domain.itinerary..",
                    "edu.itba.fieldops.domain.tracking..",
                    "edu.itba.fieldops.domain.report..",
                    "edu.itba.fieldops.domain.validation..",
                    "edu.itba.fieldops.domain.notification.."
            );

    @ArchTest
    static final ArchRule onlyMessagingIsScheduled = noMethods()
            .that().areDeclaredInClassesThat().resideOutsideOfPackage("edu.itba.fieldops.adapters.messaging..")
            .should().beAnnotatedWith(Scheduled.class);

    @ArchTest
    static final ArchRule theEventCodecIsPlainJava = noClasses()
            .that().resideInAPackage("edu.itba.fieldops.adapters.event..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "tools.jackson..",
                    "com.fasterxml.jackson.."
            );
}
