package io.quarkiverse.jberet.it.jdbc;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.Header;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
class JdbcRepositoryTest {
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
    void jdbc() {
        given()
                .get("/batch/job/execute")
                .then()
                .statusCode(200);

        given().get("/repository/instances/jdbc")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].jobName", equalTo("jdbc"));

        given().get("/jdbc/instances/jdbc")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].jobName", equalTo("jdbc"));
    }
}
