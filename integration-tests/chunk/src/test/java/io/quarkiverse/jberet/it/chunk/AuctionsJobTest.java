package io.quarkiverse.jberet.it.chunk;

import static io.quarkus.test.common.http.TestHTTPResourceManager.getUri;
import static io.restassured.RestAssured.given;
import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Properties;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkiverse.jberet.it.chunk.BatchResource.JobData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.Header;

@QuarkusTest
class AuctionsJobTest {
    @BeforeAll
    static void beforeAll() {
        RestAssured.filters(
                (requestSpec, responseSpec, ctx) -> {
                    requestSpec.header(new Header(ACCEPT, APPLICATION_JSON));
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
    void auctions() throws Exception {
        BatchClient batchClient = new BatchClient(getUri());

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

        BatchClient batchClient = new BatchClient(getUri());

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobData.getExecutionId()).getBatchStatus()));

        Auction auction = given().get("/auctions/{id}", 279573567L).then().statusCode(200).extract().as(Auction.class);
        assertNotNull(auction);
        assertEquals(3800000, auction.getBid());
        assertEquals(4000000, auction.getBuyout());
        assertEquals(22792, auction.getItemId());
        assertEquals(20, auction.getQuantity());
    }
}
