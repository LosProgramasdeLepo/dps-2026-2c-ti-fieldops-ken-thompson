package edu.itba.fieldops.api.error;

public final class FeatureDisabled extends RuntimeException {
    public FeatureDisabled(String message) {
        super(message);
    }
}
