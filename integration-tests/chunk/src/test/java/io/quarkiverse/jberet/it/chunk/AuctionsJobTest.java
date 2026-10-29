package io.quarkiverse.jberet.it.chunk;

import static io.restassured.RestAssured.given;
import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static jakarta.batch.runtime.BatchStatus.FAILED;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Properties;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkiverse.jberet.it.chunk.BatchResource.JobData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.HttpServer;
import io.restassured.RestAssured;
import io.restassured.http.Header;

@QuarkusTest
class AuctionsJobTest {
    @BeforeAll
    static void beforeAll() {
        RestAssured.filters(
                (requestSpec, responseSpec, ctx) -> {
                    requestSpec.header(new Header(ACCEPT, APPLICATION_JSON));
                    return ctx.next(requestSpec, responseSpec);
                });
    }

    @AfterAll
    static void afterAll() {
        RestAssured.reset();
    }

    HttpServer httpServer;

    @Test
    void auctions() throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());

        Properties properties = new Properties();
        properties.setProperty("auctions.file", "auctions.json");
        JobExecutionEntity auctions = batchClient.startJob("auctions", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(auctions.getExecutionId()).getBatchStatus()));

        Auction auction = given().get("/auctions/{id}", 279573567L).then().statusCode(200).extract().as(Auction.class);
        assertNotNull(auction);
        assertEquals(3800000, auction.getBid());
        assertEquals(4000000, auction.getBuyout());
        assertEquals(22792, auction.getItemId());
        assertEquals(20, auction.getQuantity());
    }

    @Test
    void batchRuntime() {
        JobData jobData = given()
                .get("/batch/job/execute/auctions.json")
                .then()
                .statusCode(200)
                .extract().as(JobData.class);

        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobData.getExecutionId()).getBatchStatus()));

        Auction auction = given().get("/auctions/{id}", 279573567L).then().statusCode(200).extract().as(Auction.class);
        assertNotNull(auction);
        assertEquals(3800000, auction.getBid());
        assertEquals(4000000, auction.getBuyout());
        assertEquals(22792, auction.getItemId());
        assertEquals(20, auction.getQuantity());
    }

    @Test
    void failedStep() throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());

        Properties properties = new Properties();
        properties.setProperty("auctions.file", "auctions.json");
        JobExecutionEntity failing = batchClient.startJob("auctions-failing", properties);

        await().atMost(5, SECONDS)
                .until(() -> FAILED.equals(batchClient.getJobExecution(failing.getExecutionId()).getBatchStatus()));

        List<String> exceptions = given().get("/batch/job/{executionId}/step-exceptions", failing.getExecutionId())
                .then().statusCode(200).extract().jsonPath().getList(".", String.class);
        assertEquals(1, exceptions.size());
        String exception = exceptions.get(0);
        assertNotNull(exception);
        assertTrue(exception.contains(AuctionProcessingException.class.getName()), exception);
        assertTrue(exception.contains("Failed to process auction"), exception);
        assertTrue(exception.contains(IllegalStateException.class.getName() + ": Invalid auction"), exception);

        // The executor must still be able to run jobs after a failure
        JobExecutionEntity auctions = batchClient.startJob("auctions", properties);
        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(auctions.getExecutionId()).getBatchStatus()));
    }
}
