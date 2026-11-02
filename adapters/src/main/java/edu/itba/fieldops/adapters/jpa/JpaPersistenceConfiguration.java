package edu.itba.fieldops.adapters.jpa;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@Profile(PersistenceProfiles.SQL)
@EnableJpaRepositories(basePackageClasses = JpaPersistenceConfiguration.class)
@EntityScan(basePackageClasses = JpaPersistenceConfiguration.class)
class JpaPersistenceConfiguration {
}
