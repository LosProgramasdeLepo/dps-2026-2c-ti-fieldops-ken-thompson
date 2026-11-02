package edu.itba.fieldops.frameworks;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "fieldops.persistence", havingValue = "jpa", matchIfMissing = true)
@EnableJpaRepositories(basePackages = "edu.itba.fieldops.adapters.jpa")
@EntityScan(basePackages = "edu.itba.fieldops.adapters.jpa")
class JpaPersistenceConfiguration {
    @Bean
    TransactionOperations transactions(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
