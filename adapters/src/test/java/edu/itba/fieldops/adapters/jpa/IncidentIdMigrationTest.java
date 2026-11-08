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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IncidentIdMigrationTest {
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
    void incidentsStoredBeforeV6GetDistinctIdsAndProposalsKeepTheirIncidentsId() throws SQLException {
        migrateTo("5");
        UUID original = UUID.randomUUID();
        UUID suggested = UUID.randomUUID();
        UUID activity = UUID.randomUUID();
        try (Connection connection = connection()) {
            insertExpedition(connection, original, 1, null, "APPROVED");
            insertExpedition(connection, suggested, 2, original, "DRAFT");
            execute(connection, "INSERT INTO expedition_runs (expedition_id, status) VALUES ('%s', 'IN_PROGRESS')".formatted(original));
            execute(connection, """
                    INSERT INTO run_incidents (expedition_id, position, description, occurred_at, activity_id) VALUES
                    ('%1$s', 0, 'flooded trail', '2026-11-01T09:00:00Z', '%2$s'),
                    ('%1$s', 1, 'radio down', '2026-11-01T10:00:00Z', NULL),
                    ('%1$s', 2, 'storm', '2026-11-01T11:00:00Z', NULL)
                    """.formatted(original, activity));
            execute(connection, """
                    INSERT INTO replan_proposals (id, original_id, suggested_id, incident_description, incident_at,
                                                  incident_activity_id, decision)
                    VALUES ('%s', '%s', '%s', 'flooded trail', '2026-11-01T09:00:00Z', '%s', 'PENDING')
                    """.formatted(UUID.randomUUID(), original, suggested, activity));
        }

        migrateTo("6");

        try (Connection connection = connection()) {
            List<UUID> incidentIds = uuids(connection, "SELECT id FROM run_incidents ORDER BY position");
            List<UUID> proposalIncident = uuids(connection, "SELECT incident_id FROM replan_proposals");
            Set<UUID> distinct = new HashSet<>(incidentIds);
            assertAll(
                    () -> assertEquals(3, incidentIds.size()),
                    () -> assertEquals(3, distinct.size()),
                    () -> assertNotNull(incidentIds.getFirst()),
                    () -> assertEquals(List.of(incidentIds.getFirst()), proposalIncident)
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
