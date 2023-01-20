package io.quarkiverse.jberet.it.scheduler;

import static io.quarkus.test.common.http.TestHTTPResourceManager.getUri;
import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.batch.runtime.BatchStatus;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
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
                },
                new RequestLoggingFilter(),
                new ResponseLoggingFilter());

    }

    @Test
    void schedule() {
        given()
                .get("/schedules/{scheduleId}/", "1")
                .then()
                .statusCode(200)
                .body("status", equalTo("SCHEDULED"));

        await().atMost(25, TimeUnit.SECONDS).until(() -> {
            List<Integer> jobExecutionIds = given()
                    .get("/schedules/{scheduleId}/", "1")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("jobExecutionIds");
            return jobExecutionIds.size() > 2;
        });

        List<Integer> jobExecutionIds = given()
                .get("/schedules/{scheduleId}/", "1")
                .then()
                .statusCode(200)
                .extract().path("jobExecutionIds");

        BatchClient batchClient = new BatchClient(getUri());
        JobExecutionEntity jobExecution = batchClient.getJobExecution(jobExecutionIds.get(0));
        assertEquals("scheduler", jobExecution.getJobName());
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
