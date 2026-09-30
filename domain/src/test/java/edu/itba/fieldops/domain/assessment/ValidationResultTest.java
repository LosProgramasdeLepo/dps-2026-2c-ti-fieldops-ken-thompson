package edu.itba.fieldops.domain.assessment;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationResultTest {
    @Test
    void distinguishesCriticalIssuesFromWarnings() {
        ExpeditionId expeditionId = new ExpeditionId(UUID.randomUUID());
        ValidationIssue critical = new ValidationIssue(IssueSeverity.CRITICAL, "PERMIT", "missing permit");
        ValidationIssue warning = new ValidationIssue(IssueSeverity.WARNING, "RISK", "high risk window");
        ValidationResult result = new ValidationResult(expeditionId, 2, List.of(critical, warning));

        assertAll(
                () -> assertEquals(expeditionId, result.expeditionId()),
                () -> assertEquals(2, result.version()),
                () -> assertTrue(result.hasCritical()),
                () -> assertEquals(List.of(warning), result.warnings())
        );
    }

    @Test
    void emptyIssueListHasNoCriticalIssues() {
        ValidationResult result = new ValidationResult(new ExpeditionId(UUID.randomUUID()), 1, List.of());

        assertFalse(result.hasCritical());
    }
}
