package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Catalog;
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
        Catalog catalog = context.catalog();
        List<ValidationIssue> issues = new ArrayList<>();
        for (Activity activity : expedition.itinerary()) {
            for (CertificationId certificationId : activity.requirements().certifications()) {
                if (uncertified(expedition, catalog, activity, certificationId)) {
                    issues.add(new ValidationIssue(
                            IssueSeverity.CRITICAL,
                            "CERTIFICATION",
                            "activity " + activity.name()
                                    + " requires certification " + certificationId
                                    + " which assigned people do not hold"
                    ));
                }
            }
        }
        return issues;
    }

    private static boolean uncertified(
            Expedition expedition,
            Catalog catalog,
            Activity activity,
            CertificationId certificationId
    ) {
        List<Person> known = new ArrayList<>();
        for (PersonAssignment assignment : expedition.assignments().peopleOf(activity.id())) {
            Optional<Person> person = catalog.person(assignment.personId());
            person.ifPresent(known::add);
        }
        return !known.isEmpty() && known.stream().noneMatch(person -> person.holds(certificationId));
    }
}
