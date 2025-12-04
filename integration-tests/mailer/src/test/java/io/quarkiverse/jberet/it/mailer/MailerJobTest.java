package io.quarkiverse.jberet.it.mailer;

import static io.quarkus.test.common.http.TestHTTPResourceManager.getUri;
import static io.restassured.RestAssured.given;
import static jakarta.batch.runtime.BatchStatus.FAILED;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
class MailerJobTest {
    @BeforeAll
    static void beforeAll() {
        RestAssured.filters(
                (requestSpec, responseSpec, ctx) -> {
                    requestSpec.header(new Header(ACCEPT, APPLICATION_JSON));
                    requestSpec.header(new Header(CONTENT_TYPE, APPLICATION_JSON));
                    return ctx.next(requestSpec, responseSpec);
                });
    }

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void setUp() {
        mailbox.clear();
    }

    @Test
    @Order(1)
    void mailedJobScheduled() {
        given()
                .get("/schedules/{scheduleId}/", "quarkus-jberet-mailer-1")
                .then()
                .statusCode(200)
                .body("status", equalTo("SCHEDULED"));

        // TODO - Programmatic client is not serializing jobExecutionIds properly in JobSchedule
        await().atMost(35, TimeUnit.SECONDS).until(() -> {
            List<Integer> jobExecutionIds = given()
                    .get("/schedules/{scheduleId}/", "quarkus-jberet-mailer-1")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("jobExecutionIds");
            return jobExecutionIds.size() > 2;
        });

        List<Mail> mails = mailbox.getMailsSentTo("batch-error@quarkiverse.org");
        assertFalse(mails.isEmpty());
    }

    @Test
    @Order(2)
    void mailerJob() throws Exception {
        BatchClient batchClient = new BatchClient(getUri());

        JobExecutionEntity auctions = batchClient.startJob("mailer", new Properties());

        await().atMost(5, SECONDS)
                .until(() -> FAILED.equals(batchClient.getJobExecution(auctions.getExecutionId()).getBatchStatus()));

        List<Mail> mails = mailbox.getMailsSentTo("batch-error@quarkiverse.org");
        assertEquals(1, mails.size());
        Mail mail = mails.get(0);
        assertEquals("mailer-test@quarkiverse.org", mail.getFrom());
        assertEquals("Batch Error", mail.getSubject());
        assertEquals("Batch Mailer failed", mail.getText());
    }
}
