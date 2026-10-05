package edu.itba.fieldops.domain.shared;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    public RiskLevel raised() {
        return switch (this) {
            case LOW -> MEDIUM;
            case MEDIUM, HIGH -> HIGH;
        };
    }
}
