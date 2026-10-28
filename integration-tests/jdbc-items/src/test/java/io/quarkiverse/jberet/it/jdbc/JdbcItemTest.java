package io.quarkiverse.jberet.it.jdbc;

import static io.restassured.RestAssured.given;
import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static jakarta.batch.runtime.BatchStatus.FAILED;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.HttpServer;
import io.restassured.RestAssured;
import io.restassured.http.Header;

@QuarkusTest
class JdbcItemTest {
    @BeforeAll
    static void beforeAll() {
        RestAssured.filters(
                (requestSpec, responseSpec, ctx) -> {
                    requestSpec.header(new Header(ACCEPT, APPLICATION_JSON));
                    return ctx.next(requestSpec, responseSpec);
                });
    }

    @BeforeEach
    void setUp() {
        given().get("/auctions/statistics/clear").then().statusCode(200);
    }

    HttpServer httpServer;

    @Test
    void jdbcProperties() throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        JobExecutionEntity jobExecution = batchClient.startJob("jdbc-properties", new Properties());

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        int count = given().get("/auctions/statistics/count").then().statusCode(200).extract().as(int.class);
        assertEquals(8, count);
    }

    @Test
    void jdbcBeans() throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        JobExecutionEntity jobExecution = batchClient.startJob("jdbc-beans", new Properties());

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        int count = given().get("/auctions/statistics/count").then().statusCode(200).extract().as(int.class);
        assertEquals(8, count);
    }

    @Test
    void jdbcRestart() throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        JobExecutionEntity jobExecution = batchClient.startJob("jdbc-restart", new Properties());

        await().atMost(5, SECONDS)
                .until(() -> FAILED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        // first chunk of 3 committed, second chunk rolled back
        int count = given().get("/auctions/statistics/count").then().statusCode(200).extract().as(int.class);
        assertEquals(3, count);

        JobExecutionEntity restartExecution = batchClient.restartJobExecution(jobExecution.getExecutionId(), new Properties());

        await().atMost(5, SECONDS)
                .until(() -> batchClient.getJobExecution(restartExecution.getExecutionId()).getEndTime() != null);
        assertEquals(COMPLETED, batchClient.getJobExecution(restartExecution.getExecutionId()).getBatchStatus());

        count = given().get("/auctions/statistics/count").then().statusCode(200).extract().as(int.class);
        assertEquals(8, count);
    }
}
