package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.expedition.Assignments;
import edu.itba.fieldops.domain.expedition.PlanningContext;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.itinerary.Activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CertificationRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(PlanningContext context) {
        Assignments assignments = context.plan().assignments();
        List<ValidationIssue> issues = new ArrayList<>();
        for (Activity activity : context.plan().activities()) {
            List<Person> known = knownAssignees(activity, assignments, context.people());
            if (!known.isEmpty()) {
                issues.addAll(missingCertifications(activity, known));
            }
        }
        return issues;
    }

    private static List<ValidationIssue> missingCertifications(Activity activity, List<Person> assignees) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (CertificationId certificationId : activity.requirements().certifications()) {
            if (assignees.stream().noneMatch(person -> person.holds(certificationId))) {
                issues.add(missing(activity, certificationId, "which assigned people do not hold"));
            }
        }
        for (CertificationId certificationId : activity.requirements().heldByEveryone()) {
            if (assignees.stream().anyMatch(person -> !person.holds(certificationId))) {
                issues.add(missing(activity, certificationId, "which is not held by every assigned person"));
            }
        }
        return issues;
    }

    private static List<Person> knownAssignees(Activity activity, Assignments assignments, People people) {
        return assignments.peopleOf(activity.id()).stream()
                .map(assignment -> people.person(assignment.personId()))
                .flatMap(Optional::stream)
                .toList();
    }

    private static ValidationIssue missing(Activity activity, CertificationId certificationId, String detail) {
        return new ValidationIssue(
                IssueSeverity.CRITICAL,
                "CERTIFICATION",
                "activity " + activity.name() + " requires certification " + certificationId + " " + detail
        );
    }
}
