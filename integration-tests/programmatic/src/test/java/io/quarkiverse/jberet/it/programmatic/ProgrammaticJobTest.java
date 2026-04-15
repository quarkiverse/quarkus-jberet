package io.quarkiverse.jberet.it.programmatic;

import static io.restassured.RestAssured.given;
import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.util.Properties;

import org.jberet.rest.client.BatchClient;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.HttpServer;

@QuarkusTest
class ProgrammaticJobTest {
    HttpServer httpServer;

    @Test
    void programmatic() {
        int executionId = Integer.parseInt(given()
                .get("/batch/job/execute")
                .then()
                .statusCode(200).extract().asString());

        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(executionId).getBatchStatus()));
    }

    @Test
    void cdi() throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        long executionId = batchClient.startJob("cdi", new Properties()).getExecutionId();

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(executionId).getBatchStatus()));
    }
}
