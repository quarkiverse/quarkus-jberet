package io.quarkiverse.jberet.it.jpa;

import static io.restassured.RestAssured.given;
import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static jakarta.batch.runtime.BatchStatus.FAILED;
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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.HttpServer;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestMethodOrder(OrderAnnotation.class)
class JpaRepositoryTest {
    HttpServer httpServer;

    @Test
    @Order(1)
    void jpa() throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
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
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());

        Properties properties = new Properties();
        properties.setProperty("jpa.batchlet.fail", "true");
        JobExecutionEntity execution = batchClient.startJob("jpa", properties);
        await().atMost(5, SECONDS)
                .until(() -> FAILED.equals(batchClient.getJobExecution(execution.getExecutionId()).getBatchStatus()));
    }

    @Test
    void restart() throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());

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
