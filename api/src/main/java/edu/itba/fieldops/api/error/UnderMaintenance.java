package edu.itba.fieldops.api.error;

public final class UnderMaintenance extends RuntimeException {
    private final int retryAfterSeconds;

    public UnderMaintenance(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    int retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
