package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationResult;

public interface ExpeditionValidator {
    ValidationResult validate(PlanningContext context);
}
