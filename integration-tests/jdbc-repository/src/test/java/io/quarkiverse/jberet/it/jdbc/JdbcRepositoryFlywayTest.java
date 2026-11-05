package io.quarkiverse.jberet.it.jdbc;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.agroal.api.AgroalDataSource;
import io.quarkiverse.jberet.it.jdbc.JdbcRepositoryFlywayTest.Profile;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * The JBeret tables are created by a Flyway migration. Flyway must run before the JBeret JDBC Repository is
 * initialized, otherwise JBeret creates the tables first and the Flyway migration fails.
 */
@QuarkusTest
@TestProfile(Profile.class)
class JdbcRepositoryFlywayTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.flyway.\"batch\".migrate-at-start", "true");
        }
    }

    @Inject
    JobOperator jobOperator;
    @Inject
    @DataSource("batch")
    AgroalDataSource dataSource;

    @Test
    void migrationOwnsTables() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(
                    "SELECT \"success\" FROM \"flyway_schema_history\" WHERE \"version\" = '1'")) {
                assertTrue(rs.next());
                assertTrue(rs.getBoolean(1));
            }

            // Column only present in the Flyway migration, not in the JBeret DDL
            try (ResultSet rs = connection.getMetaData().getColumns(null, null, "JOB_INSTANCE", "TENANT")) {
                assertTrue(rs.next());
            }
        }
    }

    @Test
    void jobRuns() throws SQLException {
        long executionId = jobOperator.start("jdbc", new Properties());
        await().atMost(5, SECONDS)
                .until(() -> BatchStatus.COMPLETED.equals(jobOperator.getJobExecution(executionId).getBatchStatus()));

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM JOB_INSTANCE WHERE JOBNAME = 'jdbc'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }
}
