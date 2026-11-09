package edu.itba.fieldops.adapters.notification;

public class SimulatedNotificationFailure extends RuntimeException {
    public SimulatedNotificationFailure() {
        super("notifications are failing on purpose: fieldops.notifications.simulate-failure is on");
    }
}
