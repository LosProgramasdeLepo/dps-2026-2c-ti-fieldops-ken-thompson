package edu.itba.fieldops.frameworks.persistence;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PersistenceProfiles.SQL)
@Import(PostgresContainerConfiguration.class)
class PostgresFlowTest extends PersistentFlow {
}
