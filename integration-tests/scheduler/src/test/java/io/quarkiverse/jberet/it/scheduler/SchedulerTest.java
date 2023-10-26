package io.quarkiverse.jberet.it.scheduler;

import static io.quarkus.test.common.http.TestHTTPResourceManager.getUri;
import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.batch.runtime.BatchStatus;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
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

    @Test
    void schedule() {
        given()
                .get("/schedules/{scheduleId}/", "1")
                .then()
                .statusCode(200)
                .body("status", equalTo("SCHEDULED"));

        await().atMost(35, TimeUnit.SECONDS).until(() -> {
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

        JobExecutionEntity jobExecutionOne = batchClient.getJobExecution(jobExecutionIds.get(0));
        assertEquals("scheduler", jobExecutionOne.getJobName());
        assertEquals(BatchStatus.COMPLETED, jobExecutionOne.getBatchStatus());

        JobExecutionEntity jobExecutionTwo = batchClient.getJobExecution(jobExecutionIds.get(1));
        assertEquals("scheduler", jobExecutionTwo.getJobName());
        assertEquals(BatchStatus.COMPLETED, jobExecutionTwo.getBatchStatus());

        JobExecutionEntity jobExecutionThree = batchClient.getJobExecution(jobExecutionIds.get(2));
        assertEquals("scheduler", jobExecutionThree.getJobName());
        assertEquals(BatchStatus.COMPLETED, jobExecutionThree.getBatchStatus());

        LocalDateTime localDateOne = jobExecutionOne.getCreateTime().toInstant().atZone(systemDefault()).toLocalDateTime()
                .truncatedTo(SECONDS);
        LocalDateTime localDateTwo = jobExecutionTwo.getCreateTime().toInstant().atZone(systemDefault()).toLocalDateTime()
                .truncatedTo(SECONDS);
        LocalDateTime localDateThree = jobExecutionThree.getCreateTime().toInstant().atZone(systemDefault()).toLocalDateTime()
                .truncatedTo(SECONDS);

        assertTrue(localDateOne.isBefore(localDateTwo));
        assertTrue(localDateTwo.isBefore(localDateThree));

        assertTrue(SECONDS.between(localDateOne, localDateTwo) >= 10);
        assertTrue(SECONDS.between(localDateTwo, localDateThree) >= 10);
    }
}
