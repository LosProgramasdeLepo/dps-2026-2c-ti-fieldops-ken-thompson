package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.usecase.shared.Clock;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionOperations;

@Configuration(proxyBeanMethods = false)
@Profile("demo")
class DemoConfiguration {
    @Bean
    ApplicationRunner demoData(FieldOps fieldOps, Clock clock, TransactionOperations transactions) {
        DemoData demo = new DemoData(fieldOps, clock);
        return arguments -> transactions.executeWithoutResult(status -> demo.loadIfEmpty());
    }
}
