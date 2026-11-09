package edu.itba.fieldops.adapters.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("fieldops.notifications")
public class NotificationProperties {
    private volatile boolean simulateFailure;

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
