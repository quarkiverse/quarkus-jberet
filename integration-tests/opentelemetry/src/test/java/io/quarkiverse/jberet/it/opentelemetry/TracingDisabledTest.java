package io.quarkiverse.jberet.it.opentelemetry;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Properties;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(TracingDisabledTest.TracingDisabledProfile.class)
class TracingDisabledTest {
    @Inject
    InMemorySpanExporter exporter;

    @Test
    void disabled() throws Exception {
        exporter.reset();
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("chunk", new Properties());
        await().atMost(5, SECONDS)
                .until(() -> jobOperator.getJobExecution(executionId).getBatchStatus() == BatchStatus.COMPLETED);

        // Give the span processor a chance to export
        Thread.sleep(200);
        assertTrue(exporter.getFinishedSpanItems().stream().noneMatch(span -> span.getName().startsWith("BatchJob")));
    }

    public static class TracingDisabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.jberet.telemetry.traces.enabled", "false");
        }
    }
}
