package io.quarkiverse.jberet.rest.deployment;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.ejb.ScheduleExpression;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduleConfigBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.Header;

class ScheduleExpressionRestTest {
    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ScheduleResource.class, ScheduleBatchlet.class)
                    .addAsResource(new StringAsset("%test.quarkus.http.port=8081\n"), "application.properties")
                    .addAsManifestResource("batchlet.xml", "batch-jobs/batchlet.xml"));

    @BeforeAll
    static void beforeAll() {
        RestAssured.filters(
                (requestSpec, responseSpec, ctx) -> {
                    requestSpec.header(new Header(CONTENT_TYPE, APPLICATION_JSON));
                    requestSpec.header(new Header(ACCEPT, APPLICATION_JSON));
                    return ctx.next(requestSpec, responseSpec);
                },
                new RequestLoggingFilter(),
                new ResponseLoggingFilter());
    }

    @Test
    void deserializationThroughCustomEndpoint() {
        JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .scheduleExpression(new ScheduleExpression().hour(6))
                .build();
        RestAssured.given()
                .body(scheduleConfig)
                .post("/schedule")
                .then()
                .statusCode(200);
    }

    @Test
    void scheduleExpressionViaJobsEndpoint() {
        RestAssured.given()
                .body("""
                        { "scheduleExpression": { "hour": "6" } }
                        """)
                .post("/jobs/batchlet/schedule")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("status", equalTo("SCHEDULED"));
    }

    @Named("batchlet")
    @Dependent
    public static class ScheduleBatchlet implements Batchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }

        @Override
        public void stop() {
        }
    }

    @ApplicationScoped
    @Path("/schedule")
    static class ScheduleResource {
        @POST
        public Response schedule(JobScheduleConfig scheduleConfig) {
            return Response.ok().build();
        }
    }
}
