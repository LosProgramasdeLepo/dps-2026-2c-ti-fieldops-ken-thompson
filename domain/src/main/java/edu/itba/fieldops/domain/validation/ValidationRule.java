package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.expedition.PlanningContext;

import java.util.List;

public interface ValidationRule {
    List<ValidationIssue> check(PlanningContext context);
}
