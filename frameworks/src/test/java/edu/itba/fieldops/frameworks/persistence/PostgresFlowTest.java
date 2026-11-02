package edu.itba.fieldops.frameworks.persistence;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("sql")
@Import(PostgresContainerConfiguration.class)
class PostgresFlowTest extends PersistentFlow {
}
