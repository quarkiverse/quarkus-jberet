package io.quarkiverse.jberet.deployment.devui;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import tools.jackson.databind.JsonNode;

/**
 * The Dev Assistant features, with an {@link Assistant} that answers with what it receives.
 */
public class JBeretDevUIAssistantTest extends DevUIJsonRPCTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(NameBatchlet.class, EchoAssistant.class, EchoAssistantProducer.class)
                    .addAsManifestResource("batchlet.xml", "batch-jobs/batchlet.xml"));

    public JBeretDevUIAssistantTest() {
        super("quarkus-jberet");
    }

    @Test
    void explainFailure() throws Exception {
        JsonNode started = executeJsonRPCMethod("startJob",
                Map.of("jobName", "batchlet", "parameters", Map.of("name", "joe")));
        long executionId = started.get("executionId").asLong();
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            for (JsonNode execution : executeJsonRPCMethod("getExecutions", Map.of("jobName", "batchlet"))) {
                if (execution.get("executionId").asLong() == executionId) {
                    return "FAILED".equals(execution.get("batchStatus").asString());
                }
            }
            return false;
        });

        JsonNode request = executeJsonRPCMethod("explainFailure", Map.of("executionId", executionId));
        assertTrue(request.get("systemMessage").asString().contains("Jakarta Batch"), request.toString());
        assertTrue(request.get("userMessage").asString().contains("ended with batch status"), request.toString());
        assertTrue(request.get("responseType").asString().endsWith("ExplainFailureResponse"), request.toString());
        assertEquals("batchlet-job", request.get("jobName").asString());
        assertEquals(String.valueOf(executionId), request.get("executionId").asString());
        assertEquals("FAILED", request.get("batchStatus").asString());
        assertTrue(request.get("parameters").asString().contains("name=joe"), request.toString());
        // The Job XML name is different from the Job id
        String jobDefinition = request.get("jobDefinition").asString();
        assertTrue(jobDefinition.contains("META-INF/batch-jobs/batchlet.xml"), jobDefinition);
        assertTrue(jobDefinition.contains("<job id=\"batchlet-job\""), jobDefinition);
        assertEquals("- `batchlet` (Batchlet): " + NameBatchlet.class.getName(), request.get("artifacts").asString());
        String steps = request.get("steps").asString();
        assertTrue(steps.contains("Step `batchlet-step`: batch status FAILED"), steps);
        assertTrue(steps.contains("Unexpected value injected to 'name': joe"), steps);
    }

    @Test
    void generateJob() throws Exception {
        JsonNode request = executeJsonRPCMethod("generateJob", Map.of("description", "Read a CSV file into a table"));
        assertTrue(request.get("systemMessage").asString().contains("Jakarta Batch"), request.toString());
        assertTrue(request.get("responseType").asString().endsWith("GenerateJobResponse"), request.toString());
        assertEquals("Read a CSV file into a table", request.get("description").asString());
        assertEquals("batchlet", request.get("jobs").asString());
        String artifacts = request.get("artifacts").asString();
        assertTrue(artifacts.contains("- `batchlet` (Batchlet): " + NameBatchlet.class.getName()), artifacts);
    }

    /**
     * Replaces the default (empty) {@link Assistant} of the Dev UI.
     */
    @Singleton
    public static class EchoAssistantProducer {
        @Produces
        @Singleton
        Optional<Assistant> assistant() {
            return Optional.of(new EchoAssistant());
        }
    }

    /**
     * Answers with the messages and variables of the request.
     */
    public static class EchoAssistant implements Assistant {
        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> CompletionStage<T> assist(
                Optional<String> systemMessage,
                String userMessage,
                Map<String, String> variables,
                List<Path> paths,
                Class<?> responseType) {
            Map<String, String> response = new HashMap<>(variables);
            response.put("systemMessage", systemMessage.orElse(""));
            response.put("userMessage", userMessage);
            response.put("responseType", responseType.getName());
            return CompletableFuture.completedFuture((T) response);
        }

        @Override
        public String getMemoryId() {
            return "jberet";
        }

        @Override
        public String getChatPath() {
            return "/";
        }
    }

    @Named("batchlet")
    @Dependent
    public static class NameBatchlet implements Batchlet {
        @Inject
        @BatchProperty(name = "name")
        String name;

        @Override
        public String process() {
            if (!"david".equals(name)) {
                throw new RuntimeException("Unexpected value injected to 'name': " + name);
            }
            return BatchStatus.COMPLETED.toString();
        }

        @Override
        public void stop() {
        }
    }
}
