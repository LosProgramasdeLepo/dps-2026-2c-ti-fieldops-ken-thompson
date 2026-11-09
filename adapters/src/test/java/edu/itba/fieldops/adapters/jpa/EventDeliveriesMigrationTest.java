package edu.itba.fieldops.adapters.jpa;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventDeliveriesMigrationTest {
    private static PostgreSQLContainer postgres;

    @BeforeAll
    static void start() {
        postgres = new PostgreSQLContainer("postgres:17-alpine");
        postgres.start();
    }

    @AfterAll
    static void stop() {
        postgres.stop();
    }

    @Test
    void repeatedIncidentsAreSeparatedAndExistingEventsCountAsDispatched() throws SQLException {
        migrateTo("7");
        UUID original = UUID.randomUUID();
        UUID incident = UUID.randomUUID();
        UUID lone = UUID.randomUUID();
        UUID firstEvent = UUID.randomUUID();
        UUID secondEvent = UUID.randomUUID();
        try (Connection connection = connection()) {
            insertExpedition(connection, original, 1, null, "APPROVED");
            insertProposal(connection, original, incident);
            insertProposal(connection, original, incident);
            insertProposal(connection, original, incident);
            insertProposal(connection, original, lone);
            insertEvent(connection, firstEvent, original);
            insertEvent(connection, secondEvent, original);
        }

        migrateTo("8");

        try (Connection connection = connection()) {
            List<UUID> incidents = uuids(connection, "SELECT incident_id FROM replan_proposals ORDER BY registration_order");
            UUID event = UUID.randomUUID();
            insertEvent(connection, event, original);
            assertAll(
                    () -> assertEquals(4, new HashSet<>(incidents).size()),
                    () -> assertEquals(incident, incidents.getFirst()),
                    () -> assertEquals(lone, incidents.getLast()),
                    () -> assertEquals(
                            new HashSet<>(List.of(firstEvent, secondEvent)),
                            new HashSet<>(uuids(connection, "SELECT event_id FROM event_fanout"))
                    ),
                    () -> assertEquals(List.of(event), uuids(connection, """
                            SELECT id FROM domain_events e
                            WHERE NOT EXISTS (SELECT 1 FROM event_fanout f WHERE f.event_id = e.id)
                            """)),
                    () -> assertThrows(SQLException.class, () -> insertProposal(connection, original, lone))
            );
        }
    }

    private static void migrateTo(String target) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load()
                .migrate();
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private static void insertExpedition(Connection connection, UUID id, int version, UUID supersedes, String status) throws SQLException {
        execute(connection, """
                INSERT INTO expeditions (id, version, supersedes_id, status, period_start, period_end)
                VALUES ('%s', %d, %s, '%s', '2026-11-01T08:00:00Z', '2026-11-06T08:00:00Z')
                """.formatted(id, version, supersedes == null ? "NULL" : "'" + supersedes + "'", status));
    }

    private static void insertProposal(Connection connection, UUID original, UUID incident) throws SQLException {
        UUID suggested = UUID.randomUUID();
        insertExpedition(connection, suggested, 2, original, "DRAFT");
        execute(connection, """
                INSERT INTO replan_proposals (id, original_id, suggested_id, incident_description, incident_at,
                                              incident_activity_id, decision, incident_id)
                VALUES ('%s', '%s', '%s', 'flooded trail', '2026-11-01T09:00:00Z', '%s', 'PENDING', '%s')
                """.formatted(UUID.randomUUID(), original, suggested, UUID.randomUUID(), incident));
    }

    private static void insertEvent(Connection connection, UUID id, UUID expedition) throws SQLException {
        execute(connection, """
                INSERT INTO domain_events (id, type, schema_version, expedition_id, occurred_at, recorded_at, payload)
                VALUES ('%s', 'RunStarted', 1, '%s', now(), now(), '{}'::jsonb)
                """.formatted(id, expedition));
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static List<UUID> uuids(Connection connection, String sql) throws SQLException {
        List<UUID> values = new ArrayList<>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(rows.getObject(1, UUID.class));
            }
        }
        return values;
    }
}
