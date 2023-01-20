package io.quarkiverse.jberet.it.programmatic;

import static io.quarkus.test.common.http.TestHTTPResourceManager.getUri;
import static io.restassured.RestAssured.given;
import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import org.jberet.rest.client.BatchClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.Header;

@QuarkusTest
class ProgrammaticJobTest {
    @BeforeAll
    static void beforeAll() {
        RestAssured.filters(
                (requestSpec, responseSpec, ctx) -> {
                    requestSpec.header(new Header(ACCEPT, TEXT_PLAIN));
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
    void programmatic() {
        int executionId = Integer.parseInt(given()
                .get("/batch/job/execute")
                .then()
                .statusCode(200).extract().asString());

        BatchClient batchClient = new BatchClient(getUri());

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(executionId).getBatchStatus()));
    }
}
