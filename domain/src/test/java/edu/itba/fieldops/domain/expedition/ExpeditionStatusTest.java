package edu.itba.fieldops.domain.expedition;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpeditionStatusTest {
    @ParameterizedTest
    @CsvSource({"DRAFT, false", "IN_REVIEW, true", "APPROVED, true", "SUPERSEDED, false"})
    void onlyAPlanUnderReviewOrApprovedOccupiesResources(ExpeditionStatus status, boolean occupies) {
        assertEquals(occupies, status.occupiesResources());
    }
}
