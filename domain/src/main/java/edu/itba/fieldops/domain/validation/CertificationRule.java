package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.People;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.itinerary.Activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CertificationRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(ValidationContext context) {
        Expedition expedition = context.expedition();
        People people = context.people();
        List<ValidationIssue> issues = new ArrayList<>();
        for (Activity activity : expedition.itinerary()) {
            List<Person> known = knownAssignees(expedition, people, activity);
            for (CertificationId certificationId : activity.requirements().certifications()) {
                if (!known.isEmpty() && known.stream().noneMatch(person -> person.holds(certificationId))) {
                    issues.add(missing(activity, certificationId, "which assigned people do not hold"));
                }
            }
            for (CertificationId certificationId : activity.requirements().heldByEveryone()) {
                if (!known.isEmpty() && known.stream().anyMatch(person -> !person.holds(certificationId))) {
                    issues.add(missing(activity, certificationId, "which is not held by every assigned person"));
                }
            }
        }
        return issues;
    }

    private static List<Person> knownAssignees(Expedition expedition, People people, Activity activity) {
        List<Person> known = new ArrayList<>();
        for (PersonAssignment assignment : expedition.assignments().peopleOf(activity.id())) {
            Optional<Person> person = people.person(assignment.personId());
            person.ifPresent(known::add);
        }
        return known;
    }

    private static ValidationIssue missing(Activity activity, CertificationId certificationId, String detail) {
        return new ValidationIssue(
                IssueSeverity.CRITICAL,
                "CERTIFICATION",
                "activity " + activity.name() + " requires certification " + certificationId + " " + detail
        );
    }
}
