package edu.itba.fieldops.domain.shared;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskLevelTest {
    @ParameterizedTest
    @CsvSource({"LOW, MEDIUM", "MEDIUM, HIGH", "HIGH, HIGH"})
    void raisingARiskMovesItOneLevelUpToHigh(RiskLevel risk, RiskLevel raised) {
        assertEquals(raised, risk.raised());
    }
}
