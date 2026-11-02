package edu.itba.fieldops.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fieldops")
public class FieldOpsProperties {
    private final Features features = new Features();
    private final Maintenance maintenance = new Maintenance();

    public Features getFeatures() {
        return features;
    }

    public Maintenance getMaintenance() {
        return maintenance;
    }

    public static class Features {
        private volatile boolean assignmentSuggestions = true;

        public boolean isAssignmentSuggestions() {
            return assignmentSuggestions;
        }

        public void setAssignmentSuggestions(boolean assignmentSuggestions) {
            this.assignmentSuggestions = assignmentSuggestions;
        }
    }

    public static class Maintenance {
        private volatile boolean enabled;
        private volatile String message = "FieldOps is under maintenance. Please try again later.";
        private volatile int retryAfterSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getRetryAfterSeconds() {
            return retryAfterSeconds;
        }

        public void setRetryAfterSeconds(int retryAfterSeconds) {
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }
}
