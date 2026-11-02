package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.AcceptWarningRequest;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.IssueResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ValidationResponse;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.Justification;
import edu.itba.fieldops.domain.identity.PersonId;

final class ReviewMapping {
    private ReviewMapping() {
    }

    static ValidationResponse validation(ValidationResult result) {
        return new ValidationResponse(
                result.expeditionId().value(),
                result.version(),
                result.issues().stream().map(ReviewMapping::issue).toList()
        );
    }

    static AcceptedWarning warning(AcceptWarningRequest request) {
        return new AcceptedWarning(
                new ValidationIssue(request.issue().severity(), request.issue().code(), request.issue().message()),
                new Justification(request.justification()),
                new PersonId(request.acceptedBy())
        );
    }

    private static IssueResponse issue(ValidationIssue issue) {
        return new IssueResponse(issue.severity().name(), issue.code(), issue.message());
    }
}
