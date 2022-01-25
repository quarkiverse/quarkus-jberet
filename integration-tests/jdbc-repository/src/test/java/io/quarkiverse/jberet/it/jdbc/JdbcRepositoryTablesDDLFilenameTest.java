package io.quarkiverse.jberet.it.jdbc;

import static io.restassured.RestAssured.given;

import java.sql.SQLException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.jberet.it.jdbc.JdbcRepositoryTablesDDLFilenameTest.Profile;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestProfile(Profile.class)
class JdbcRepositoryTablesDDLFilenameTest extends JdbcRepositoryTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "test-ddl-filename";
        }
    }

    @ParameterizedTest(name = "{argumentsWithNames}")
    @ValueSource(strings = { "jb_test_schema.JOB_INSTANCE", "jb_test_schema.JOB_EXECUTION", "jb_test_schema.STEP_EXECUTION",
            "jb_test_schema.PARTITION_EXECUTION" })
    void testTables(String tableName) throws SQLException {
        given().get("/jdbc/tables/" + tableName)
                .then()
                .statusCode(200);
    }
}
