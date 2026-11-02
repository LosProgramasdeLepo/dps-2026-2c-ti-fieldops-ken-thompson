package edu.itba.fieldops.adapters.mongo;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration(proxyBeanMethods = false)
@Profile(PersistenceProfiles.MONGO)
@EnableMongoRepositories(basePackageClasses = MongoPersistenceConfiguration.class)
class MongoPersistenceConfiguration {
    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory databases) {
        return new MongoTransactionManager(databases);
    }
}
