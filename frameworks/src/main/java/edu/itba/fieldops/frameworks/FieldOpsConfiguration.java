package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.messaging.EventsProperties;
import edu.itba.fieldops.adapters.notification.NotificationProperties;
import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.catalog.ConsumableRegistry;
import edu.itba.fieldops.domain.catalog.InstrumentRegistry;
import edu.itba.fieldops.domain.catalog.PermitRegistry;
import edu.itba.fieldops.domain.catalog.PersonRegistry;
import edu.itba.fieldops.domain.catalog.VehicleRegistry;
import edu.itba.fieldops.domain.expedition.AssignmentSuggester;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionApprover;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionTracker;
import edu.itba.fieldops.domain.expedition.ExpeditionValidator;
import edu.itba.fieldops.domain.expedition.IncidentResponder;
import edu.itba.fieldops.domain.expedition.PlanningContexts;
import edu.itba.fieldops.domain.expedition.ProposalReviewer;
import edu.itba.fieldops.domain.expedition.Proposals;
import edu.itba.fieldops.domain.expedition.Replanner;
import edu.itba.fieldops.domain.notification.ResponsiblesToNotify;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.validation.RuleBasedValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties({EventsProperties.class, NotificationProperties.class})
class FieldOpsConfiguration {
    @Bean
    Catalogs catalogs(
            PersonRegistry people,
            VehicleRegistry vehicles,
            InstrumentRegistry instruments,
            ConsumableRegistry consumables,
            PermitRegistry permits
    ) {
        return new Catalogs(new BookableResources(people, vehicles, instruments), consumables, permits);
    }

    @Bean
    PlanningContexts planningContexts(ExpeditionRepository plans, ExecutionRepository runs, Catalogs catalogs) {
        return new PlanningContexts(plans, runs, catalogs);
    }

    @Bean
    ExpeditionValidator validator() {
        return RuleBasedValidator.withDefaultRules();
    }

    @Bean
    AssignmentSuggester suggester() {
        return new AssignmentSuggester();
    }

    @Bean
    Replanner replanner(AssignmentSuggester suggester, PlanningContexts contexts) {
        return new Replanner(suggester, contexts);
    }

    @Bean
    IncidentResponder incidentResponder(ExpeditionRepository plans, Proposals proposals, Replanner replanner, Clock clock) {
        return new IncidentResponder(plans, proposals, replanner, clock);
    }

    @Bean
    ExpeditionApprover expeditionApprover(ExpeditionValidator validator, PlanningContexts contexts, Clock clock) {
        return new ExpeditionApprover(validator, contexts, clock);
    }

    @Bean
    ExpeditionTracker tracker(ExpeditionRepository plans, ExecutionRepository runs, ConsumableRegistry consumables, Clock clock) {
        return new ExpeditionTracker(plans, runs, consumables, clock);
    }

    @Bean
    ProposalReviewer proposalReviewer(ExpeditionRepository plans, Clock clock) {
        return new ProposalReviewer(plans, clock);
    }

    @Bean
    ResponsiblesToNotify responsiblesToNotify(ExpeditionRepository plans) {
        return new ResponsiblesToNotify(plans);
    }
}
