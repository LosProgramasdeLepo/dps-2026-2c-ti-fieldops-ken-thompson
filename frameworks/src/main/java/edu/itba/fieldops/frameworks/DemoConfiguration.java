package edu.itba.fieldops.frameworks;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionOperations;

@Configuration(proxyBeanMethods = false)
@Profile("demo")
class DemoConfiguration {
    @Bean
    ApplicationRunner loadDemoData(DemoData demo, TransactionOperations transactions) {
        return arguments -> transactions.executeWithoutResult(status -> demo.loadIfEmpty());
    }
}
