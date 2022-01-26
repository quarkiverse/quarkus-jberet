package io.quarkiverse.jberet.it.jdbc;

import static io.restassured.RestAssured.given;

import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.jberet.it.jdbc.JdbcRepositoryTablesPrefixSuffixTest.Profile;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestProfile(Profile.class)
class JdbcRepositoryTablesPrefixSuffixTest extends JdbcRepositoryTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.jberet.repository.jdbc.db-table-prefix", "jb_prefix_",
                    "quarkus.jberet.repository.jdbc.db-table-suffix", "_jb_suffix",
                    "job-instance-tablename", "jb_prefix_JOB_INSTANCE_jb_suffix");
        }
    }

    @ParameterizedTest(name = "{argumentsWithNames}")
    @ValueSource(strings = { "jb_prefix_JOB_INSTANCE_jb_suffix", "jb_prefix_JOB_EXECUTION_jb_suffix",
            "jb_prefix_STEP_EXECUTION_jb_suffix", "jb_prefix_PARTITION_EXECUTION_jb_suffix" })
    void testTables(String tableName) throws SQLException {
        given().get("/jdbc/tables/" + tableName)
                .then()
                .statusCode(200);
    }
}
