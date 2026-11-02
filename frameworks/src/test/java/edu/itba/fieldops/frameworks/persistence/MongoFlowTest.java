package edu.itba.fieldops.frameworks.persistence;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("mongo")
@Import(MongoContainerConfiguration.class)
class MongoFlowTest extends PersistentFlow {
}
