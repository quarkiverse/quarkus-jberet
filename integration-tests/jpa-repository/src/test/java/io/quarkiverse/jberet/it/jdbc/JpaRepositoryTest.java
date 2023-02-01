package io.quarkiverse.jberet.it.jdbc;

import static io.quarkus.test.common.http.TestHTTPResourceManager.getUri;
import static io.restassured.RestAssured.given;
import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static jakarta.batch.runtime.BatchStatus.FAILED;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Properties;

import jakarta.enterprise.inject.Alternative;

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
@Alternative
class JpaRepositoryTest {

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
    void jpa() throws Exception {
        BatchClient batchClient = new BatchClient(getUri());
        JobExecutionEntity execution = batchClient.startJob("jpa", new Properties());
        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(execution.getExecutionId()).getBatchStatus()));

        given().get("/repository/instances/jpa")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].jobName", equalTo("jpa"));

        given().get("/jpa/instances/jpa")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].jobName", equalTo("jpa"));
    }

    @Test
    void fail() throws Exception {
        BatchClient batchClient = new BatchClient(getUri());

        Properties properties = new Properties();
        properties.setProperty("jpa.batchlet.fail", "true");
        JobExecutionEntity execution = batchClient.startJob("jpa", properties);
        await().atMost(5, SECONDS)
                .until(() -> FAILED.equals(batchClient.getJobExecution(execution.getExecutionId()).getBatchStatus()));
    }

    @Test
    void restart() throws Exception {
        BatchClient batchClient = new BatchClient(getUri());

        Properties properties = new Properties();
        properties.setProperty("jpa.batchlet.fail", "true");
        JobExecutionEntity execution = batchClient.startJob("jpa", properties);
        await().atMost(5, SECONDS)
                .until(() -> FAILED.equals(batchClient.getJobExecution(execution.getExecutionId()).getBatchStatus()));

        // Remove Job from cache
        given().delete("/repository/jobs/{jobId}", "jpa")
                .then()
                .statusCode(204);

        properties.setProperty("jpa.batchlet.fail", "false");
        JobExecutionEntity restart = batchClient.restartJobExecution(execution.getExecutionId(), properties);
        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(restart.getExecutionId()).getBatchStatus()));
    }
}
