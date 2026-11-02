package edu.itba.fieldops.frameworks.persistence;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PersistenceProfiles.MONGO)
@Import(MongoContainerConfiguration.class)
class MongoFlowTest extends PersistentFlow {
}
