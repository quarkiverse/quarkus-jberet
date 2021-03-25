package io.quarkiverse.jberet.it.jdbc;

import static io.quarkus.test.common.http.TestHTTPResourceManager.getUri;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.batch.runtime.BatchStatus.COMPLETED;
import static javax.batch.runtime.BatchStatus.FAILED;
import static javax.batch.runtime.BatchStatus.STARTED;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.Header;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestMethodOrder(OrderAnnotation.class)
class JdbcRepositoryTest {
    @BeforeAll
    static void beforeAll() {
        RestAssured.filters(
                (requestSpec, responseSpec, ctx) -> {
                    requestSpec.header(new Header(ACCEPT, APPLICATION_JSON));
                    requestSpec.header(new Header(CONTENT_TYPE, APPLICATION_JSON));
                    return ctx.next(requestSpec, responseSpec);
                },
                new RequestLoggingFilter(),
                new ResponseLoggingFilter());

    }

    @AfterAll
    static void afterAll() {
        RestAssured.reset();
    }

    @Test
    @Order(1)
    void jdbc() throws Exception {
        BatchClient batchClient = new BatchClient(getUri());
        JobExecutionEntity execution = batchClient.startJob("jdbc", new Properties());
        assertEquals(STARTED, execution.getBatchStatus());
        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(execution.getExecutionId()).getBatchStatus()));

        given().get("/repository/instances/jdbc")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].jobName", equalTo("jdbc"));

        given().get("/jdbc/instances/jdbc")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].jobName", equalTo("jdbc"));
    }

    @Test
    void fail() throws Exception {
        BatchClient batchClient = new BatchClient(getUri());

        Properties properties = new Properties();
        properties.setProperty("jdbc.batchlet.fail", "true");
        JobExecutionEntity execution = batchClient.startJob("jdbc", properties);
        assertEquals(STARTED, execution.getBatchStatus());
        await().atMost(5, SECONDS)
                .until(() -> FAILED.equals(batchClient.getJobExecution(execution.getExecutionId()).getBatchStatus()));
    }

    @Test
    void restart() throws Exception {
        BatchClient batchClient = new BatchClient(getUri());

        Properties properties = new Properties();
        properties.setProperty("jdbc.batchlet.fail", "true");
        JobExecutionEntity execution = batchClient.startJob("jdbc", properties);
        assertEquals(STARTED, execution.getBatchStatus());
        await().atMost(5, SECONDS)
                .until(() -> FAILED.equals(batchClient.getJobExecution(execution.getExecutionId()).getBatchStatus()));

        // Remove Job from cache
        given().delete("/repository/jobs/{jobId}", "jdbc")
                .then()
                .statusCode(204);

        properties.setProperty("jdbc.batchlet.fail", "false");
        JobExecutionEntity restart = batchClient.restartJobExecution(execution.getExecutionId(), properties);
        assertEquals(STARTED, restart.getBatchStatus());
        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(restart.getExecutionId()).getBatchStatus()));
    }
}
