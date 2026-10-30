package io.quarkiverse.jberet.it.mongo.repository;

import static io.restassured.RestAssured.given;
import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static jakarta.batch.runtime.BatchStatus.FAILED;
import static jakarta.batch.runtime.BatchStatus.STARTED;
import static jakarta.batch.runtime.BatchStatus.STOPPED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Properties;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.HttpServer;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
class MongoRepositoryTest {
    HttpServer httpServer;

    @Test
    @Order(1)
    void complete() throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        JobExecutionEntity execution = batchClient.startJob("mongo-batchlet", new Properties());
        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(execution.getExecutionId()).getBatchStatus()));

        given().get("/repository/instances/mongo-batchlet")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].jobName", equalTo("mongo-batchlet"));
    }

    @Test
    void restart() throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());

        Properties properties = new Properties();
        properties.setProperty("mongo.batchlet.fail", "true");
        JobExecutionEntity execution = batchClient.startJob("mongo-batchlet", properties);
        await().atMost(5, SECONDS)
                .until(() -> FAILED.equals(batchClient.getJobExecution(execution.getExecutionId()).getBatchStatus()));

        // Remove Job from cache
        given().delete("/repository/jobs/{jobId}", "mongo-batchlet")
                .then()
                .statusCode(204);

        properties.setProperty("mongo.batchlet.fail", "false");
        JobExecutionEntity restart = batchClient.restartJobExecution(execution.getExecutionId(), properties);
        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(restart.getExecutionId()).getBatchStatus()));
    }

    @Test
    void stop() throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        JobExecutionEntity execution = batchClient.startJob("mongo-stop", new Properties());
        await().atMost(5, SECONDS)
                .until(() -> STARTED.equals(batchClient.getJobExecution(execution.getExecutionId()).getBatchStatus()));

        batchClient.stopJobExecution(execution.getExecutionId());
        await().atMost(5, SECONDS)
                .until(() -> STOPPED.equals(batchClient.getJobExecution(execution.getExecutionId()).getBatchStatus()));
    }
}
