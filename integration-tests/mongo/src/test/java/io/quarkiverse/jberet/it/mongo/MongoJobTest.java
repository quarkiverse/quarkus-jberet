package io.quarkiverse.jberet.it.mongo;

import static io.restassured.RestAssured.given;
import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Properties;

import jakarta.inject.Inject;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.HttpServer;

@QuarkusTest
class MongoJobTest {
    @Inject
    HttpServer server;

    @Test
    void chunk() throws Exception {
        // TODO - Serialization error when Job not found - Not related with Mongo, but check
        BatchClient batchClient = new BatchClient(server.getLocalBaseUri().toString());
        Properties properties = new Properties();
        properties.setProperty("auctions.file", "auctions.json");
        JobExecutionEntity jobExecution = batchClient.startJob("mongo", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        Auction auction = given().get("/auctions/default/{id}", 279573567L).then().statusCode(200).extract().as(Auction.class);
        assertNotNull(auction);
        assertEquals(3800000, auction.getBid());
        assertEquals(4000000, auction.getBuyout());
        assertEquals(22792, auction.getItemId());
        assertEquals(20, auction.getQuantity());
    }

    @Test
    void chunkClientNamed() throws Exception {
        BatchClient batchClient = new BatchClient(server.getLocalBaseUri().toString());
        Properties properties = new Properties();
        properties.setProperty("auctions.file", "auctions.json");
        JobExecutionEntity jobExecution = batchClient.startJob("mongo-named", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        Auction auction = given().get("/auctions/aggra/{id}", 279573567L).then().statusCode(200).extract().as(Auction.class);
        assertNotNull(auction);
        assertEquals(3800000, auction.getBid());
        assertEquals(4000000, auction.getBuyout());
        assertEquals(22792, auction.getItemId());
        assertEquals(20, auction.getQuantity());
    }

    @Test
    void chunkNoClientInjection() throws Exception {
        BatchClient batchClient = new BatchClient(server.getLocalBaseUri().toString());
        Properties properties = new Properties();
        properties.setProperty("auctions.file", "auctions.json");
        JobExecutionEntity jobExecution = batchClient.startJob("mongo-no-injection", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));
    }

    @Test
    void mongoBeans() throws Exception {
        BatchClient batchClient = new BatchClient(server.getLocalBaseUri().toString());
        Properties properties = new Properties();
        properties.setProperty("auctions.file", "auctions.json");
        JobExecutionEntity jobExecution = batchClient.startJob("mongo-beans", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        Auction[] auctions = given().get("/auctions/beans").then().statusCode(200).extract().as(Auction[].class);
        assertEquals(2, auctions.length);
    }

    @Test
    void mongoSkip() throws Exception {
        BatchClient batchClient = new BatchClient(server.getLocalBaseUri().toString());
        Properties properties = new Properties();
        properties.setProperty("auctions.file", "auctions.json");
        JobExecutionEntity jobExecution = batchClient.startJob("mongo-skip", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        Auction[] auctions = given().get("/auctions/skip").then().statusCode(200).extract().as(Auction[].class);
        assertEquals(6, auctions.length);
    }
}
