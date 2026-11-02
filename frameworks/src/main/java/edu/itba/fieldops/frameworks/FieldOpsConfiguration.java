package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.catalog.ConsumableRegistry;
import edu.itba.fieldops.domain.catalog.InstrumentRegistry;
import edu.itba.fieldops.domain.catalog.PermitRegistry;
import edu.itba.fieldops.domain.catalog.PersonRegistry;
import edu.itba.fieldops.domain.catalog.VehicleRegistry;
import edu.itba.fieldops.domain.expedition.AssignmentSuggester;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionValidator;
import edu.itba.fieldops.domain.expedition.PlanningContexts;
import edu.itba.fieldops.domain.expedition.Replanner;
import edu.itba.fieldops.domain.validation.RuleBasedValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
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
    ExpeditionValidator validator() {
        return RuleBasedValidator.withDefaultRules();
    }

    @Bean
    AssignmentSuggester suggester() {
        return new AssignmentSuggester();
    }

    @Bean
    Replanner replanner(AssignmentSuggester suggester, ExpeditionRepository plans, ExecutionRepository runs, Catalogs catalogs) {
        return new Replanner(suggester, new PlanningContexts(plans, runs, catalogs));
    }
}
