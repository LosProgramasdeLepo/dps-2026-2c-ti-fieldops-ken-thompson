package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.usecase.shared.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionOperations;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "fieldops.persistence", havingValue = "memory")
class InMemoryPersistenceConfiguration {
    @Bean
    TransactionOperations transactions() {
        return TransactionOperations.withoutTransaction();
    }

    @Bean
    FieldOpsRepositories repositories(Clock clock) {
        return new InMemoryFieldOps(clock).repositories();
    }
}
