package io.quarkiverse.jberet.it.scheduler;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.batch.runtime.BatchStatus;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.HttpServer;
import io.restassured.RestAssured;
import io.restassured.http.Header;

@QuarkusTest
class SchedulerTest {
    @BeforeAll
    static void beforeAll() {
        RestAssured.filters(
                (requestSpec, responseSpec, ctx) -> {
                    requestSpec.header(new Header(ACCEPT, APPLICATION_JSON));
                    requestSpec.header(new Header(CONTENT_TYPE, APPLICATION_JSON));
                    return ctx.next(requestSpec, responseSpec);
                });
    }

    @AfterAll
    static void afterAll() {
        RestAssured.reset();
    }

    HttpServer httpServer;

    @Test
    void schedule() {
        given()
                .get("/schedules/{scheduleId}/", "quarkus-jberet-scheduler-1")
                .then()
                .statusCode(200)
                .body("status", equalTo("SCHEDULED"));

        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());

        await().atMost(60, TimeUnit.SECONDS).until(() -> {
            List<Integer> jobExecutionIds = given()
                    .get("/schedules/{scheduleId}/", "quarkus-jberet-scheduler-1")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("jobExecutionIds");
            if (jobExecutionIds.size() < 3) {
                return false;
            }
            for (int i = 0; i < 3; i++) {
                if (batchClient.getJobExecution(jobExecutionIds.get(i)).getBatchStatus() != BatchStatus.COMPLETED) {
                    return false;
                }
            }
            return true;
        });

        List<Integer> jobExecutionIds = given()
                .get("/schedules/{scheduleId}/", "quarkus-jberet-scheduler-1")
                .then()
                .statusCode(200)
                .extract().path("jobExecutionIds");

        JobExecutionEntity jobExecutionOne = batchClient.getJobExecution(jobExecutionIds.get(0));
        assertEquals("scheduler", jobExecutionOne.getJobName());
        assertEquals(BatchStatus.COMPLETED, jobExecutionOne.getBatchStatus());

        JobExecutionEntity jobExecutionTwo = batchClient.getJobExecution(jobExecutionIds.get(1));
        assertEquals("scheduler", jobExecutionTwo.getJobName());
        assertEquals(BatchStatus.COMPLETED, jobExecutionTwo.getBatchStatus());

        JobExecutionEntity jobExecutionThree = batchClient.getJobExecution(jobExecutionIds.get(2));
        assertEquals("scheduler", jobExecutionThree.getJobName());
        assertEquals(BatchStatus.COMPLETED, jobExecutionThree.getBatchStatus());

        Instant createTimeOne = jobExecutionOne.getCreateTime().toInstant();
        Instant createTimeTwo = jobExecutionTwo.getCreateTime().toInstant();
        Instant createTimeThree = jobExecutionThree.getCreateTime().toInstant();

        // The cron fires every 10 seconds, but the Scheduler only checks triggers once per second, so a fire can be
        // late. Only verify that the executions follow the cron, and not the exact period between them.
        assertTrue(SECONDS.between(createTimeOne, createTimeTwo) >= 5);
        assertTrue(SECONDS.between(createTimeTwo, createTimeThree) >= 5);
    }
}
