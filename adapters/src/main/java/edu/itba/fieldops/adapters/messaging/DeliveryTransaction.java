package edu.itba.fieldops.adapters.messaging;

public interface DeliveryTransaction {
    void run(Runnable delivery);
}
