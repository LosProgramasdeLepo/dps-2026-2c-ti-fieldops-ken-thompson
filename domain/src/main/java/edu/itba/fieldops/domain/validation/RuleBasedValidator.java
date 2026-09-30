package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.expedition.ExpeditionValidator;
import edu.itba.fieldops.domain.expedition.PlanningContext;

import java.util.List;
import java.util.Objects;

public final class RuleBasedValidator implements ExpeditionValidator {
    private final List<ValidationRule> rules;

    public RuleBasedValidator(List<ValidationRule> rules) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
    }

    public static RuleBasedValidator withDefaultRules() {
        return new RuleBasedValidator(List.of(
                new MissingResourceRule(),
                new TemporalOverlapRule(),
                new StockRule(),
                new CertificationRule(),
                new CapacityRule(),
                new PermitRule(),
                new ParallelAssignmentRule()
        ));
    }

    @Override
    public ValidationResult validate(PlanningContext context) {
        return new ValidationResult(
                context.plan().id(),
                context.plan().version(),
                rules.stream()
                        .map(rule -> rule.check(context))
                        .flatMap(List::stream)
                        .toList()
        );
    }
}
