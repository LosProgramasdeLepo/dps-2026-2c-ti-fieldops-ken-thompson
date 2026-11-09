package edu.itba.fieldops.adapters.messaging;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile({PersistenceProfiles.SQL, PersistenceProfiles.MONGO})
public class RequiresNewDeliveryTransaction implements DeliveryTransaction {
    private final TransactionTemplate transaction;

    public RequiresNewDeliveryTransaction(PlatformTransactionManager transactions) {
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void run(Runnable delivery) {
        transaction.executeWithoutResult(status -> delivery.run());
    }
}
