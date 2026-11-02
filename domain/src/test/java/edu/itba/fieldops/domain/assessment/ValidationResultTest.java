package edu.itba.fieldops.domain.assessment;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationResultTest {
    @Test
    void distinguishesCriticalIssuesFromWarnings() {
        ValidationIssue critical = new ValidationIssue(IssueSeverity.CRITICAL, "PERMIT", "missing permit");
        ValidationIssue warning = new ValidationIssue(IssueSeverity.WARNING, "RISK", "high risk window");
        ValidationResult result = new ValidationResult(new ExpeditionId(UUID.randomUUID()), 2, List.of(critical, warning));

        assertAll(
                () -> assertTrue(result.hasCritical()),
                () -> assertEquals(List.of(warning), result.warnings())
        );
    }
}
