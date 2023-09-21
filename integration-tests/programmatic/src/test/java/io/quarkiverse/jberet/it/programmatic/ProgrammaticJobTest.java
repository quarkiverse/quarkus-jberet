package io.quarkiverse.jberet.it.programmatic;

import static io.quarkus.test.common.http.TestHTTPResourceManager.getUri;
import static io.restassured.RestAssured.given;
import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.util.Properties;

import org.jberet.rest.client.BatchClient;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ProgrammaticJobTest {

    @Test
    void programmatic() {
        int executionId = Integer.parseInt(given()
                .get("/batch/job/execute")
                .then()
                .statusCode(200).extract().asString());

        BatchClient batchClient = new BatchClient(getUri());

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(executionId).getBatchStatus()));
    }

    @Test
    void cdi() throws Exception {
        BatchClient batchClient = new BatchClient(getUri());
        long executionId = batchClient.startJob("cdi", new Properties()).getExecutionId();

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(executionId).getBatchStatus()));
    }
}
