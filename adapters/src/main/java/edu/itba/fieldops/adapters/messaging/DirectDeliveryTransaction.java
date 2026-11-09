package edu.itba.fieldops.adapters.messaging;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(PersistenceProfiles.MEMORY)
public class DirectDeliveryTransaction implements DeliveryTransaction {
    @Override
    public void run(Runnable delivery) {
        delivery.run();
    }
}
