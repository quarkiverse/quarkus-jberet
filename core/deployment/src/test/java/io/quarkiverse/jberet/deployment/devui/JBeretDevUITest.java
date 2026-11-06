package io.quarkiverse.jberet.deployment.devui;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.batch.api.Decider;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.StepExecution;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketClientOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class JBeretDevUITest extends DevUIJsonRPCTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(NameBatchlet.class, NoopBatchlet.class, SpareBatchlet.class, FlowDecider.class,
                            WaitBatchlet.class)
                    // The Job ids are different from the Job XML names
                    .addAsManifestResource("batchlet.xml", "batch-jobs/batchlet.xml")
                    .addAsManifestResource("devui-flow.xml", "batch-jobs/flow.xml")
                    .addAsManifestResource("devui-broken.xml", "batch-jobs/broken.xml")
                    .addAsManifestResource("devui-stoppable.xml", "batch-jobs/stoppable.xml")
                    .addAsResource(new StringAsset("""
                            quarkus.jberet.job.batchlet.params.name=david
                            quarkus.jberet.job.batchlet.cron=0 0 0 1 1 ?
                            """), "application.properties"));

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TestHTTPResource("/q/dev-ui/json-rpc-ws")
    URI jsonRpcUri;

    public JBeretDevUITest() {
        super("quarkus-jberet");
    }

    @Test
    void jobs() throws Exception {
        assertEquals(4, executeJsonRPCMethod(Integer.class, "getJobCount"));

        JsonNode jobs = executeJsonRPCMethod("getJobs");
        assertEquals(4, jobs.size());
        JsonNode job = jobs.get(0);
        assertEquals("batchlet", job.get("name").asString());
        assertEquals("batchlet-job", job.get("id").asString());
        assertTrue(job.get("restartable").asBoolean());
        assertEquals(1, job.get("steps").size());
        assertEquals("batchlet-step", job.get("steps").get(0).asString());
        assertEquals("david", job.get("parameters").get("name").asString());
        assertEquals("0 0 0 1 1 ?", job.get("cron").asString());

        JsonNode flow = jobs.get(2);
        assertEquals("flow", flow.get("name").asString());
        assertEquals("flow-job", flow.get("id").asString());
        assertEquals(List.of("prepare", "left-step", "right-step", "report"), strings(flow.get("steps")));
    }

    @Test
    void batchArtifacts() throws Exception {
        Map<String, JsonNode> artifacts = new java.util.HashMap<>();
        for (JsonNode artifact : executeJsonRPCMethod("getBatchArtifacts")) {
            artifacts.put(artifact.get("ref").asString(), artifact);
        }

        JsonNode batchlet = artifacts.get("batchlet");
        assertEquals("RESOLVED", batchlet.get("status").asString());
        assertEquals(NameBatchlet.class.getName(), batchlet.get("className").asString());
        assertEquals(List.of("Batchlet"), strings(batchlet.get("types")));
        assertFalse(batchlet.get("builtIn").asBoolean());
        assertEquals(1, batchlet.get("usages").size());
        assertEquals("batchlet", batchlet.get("usages").get(0).get("jobName").asString());
        assertEquals("batchlet-step", batchlet.get("usages").get(0).get("element").asString());
        assertEquals("Batchlet", batchlet.get("usages").get(0).get("role").asString());

        assertEquals(4, artifacts.get("noop").get("usages").size());
        JsonNode decider = artifacts.get("flowDecider");
        assertEquals(List.of("Decider"), strings(decider.get("types")));
        assertEquals("decide", decider.get("usages").get(0).get("element").asString());

        JsonNode missing = artifacts.get("missingReader");
        assertEquals("UNRESOLVED", missing.get("status").asString());
        assertTrue(missing.get("className") == null || missing.get("className").isNull());
        assertEquals("Reader", missing.get("usages").get(0).get("role").asString());

        assertEquals("EXPRESSION", artifacts.get("#{jobParameters['writer']}").get("status").asString());

        // Available, but not used by any job
        JsonNode spare = artifacts.get("spareBatchlet");
        assertNotNull(spare);
        assertEquals(0, spare.get("usages").size());
        assertEquals(SpareBatchlet.class.getName(), spare.get("className").asString());

        // The Dev UI listeners are not batch artifacts of the application
        assertTrue(artifacts.keySet().stream().noneMatch(ref -> ref.contains("JBeretDev")), artifacts.keySet().toString());
    }

    @Test
    void jobDiagram() throws Exception {
        String dot = executeJsonRPCMethod(String.class, "getJobDiagram", Map.of("jobName", "flow"));
        assertTrue(dot.startsWith("digraph \"flow-job\""), dot);
        assertTrue(dot.contains("\"step:prepare\" -> \"decision:decide\""), dot);
        assertTrue(dot.contains("\"decision:decide\" -> \"fork:parallel\" [label=\"split\"]"), dot);
        assertTrue(dot.contains("label=\"fail\""), dot);
        assertTrue(dot.contains("exit: FAILED_BY_DECISION"), dot);
        assertTrue(dot.contains("subgraph \"cluster_split:parallel\""), dot);
        assertTrue(dot.contains("\"fork:parallel\" -> \"step:left-step\" [lhead=\"cluster_flow:left\"]"), dot);
        assertTrue(dot.contains("\"step:right-step\" -> \"join:parallel\" [ltail=\"cluster_flow:right\"]"), dot);
        assertTrue(dot.contains("\"join:parallel\" -> \"step:report\""), dot);
        assertTrue(dot.contains("\"step:report\" -> \"__end\""), dot);
        // The decision has a catch-all transition, so it never reaches the implicit end
        assertFalse(dot.contains("\"decision:decide\" -> \"__end\""), dot);
        // The Dev listeners are not part of the diagram
        assertFalse(dot.contains("JBeretDev"), dot);

        assertTrue(executeJsonRPCMethod("getJobDiagram", Map.of("jobName", "unknown")).isNull());
    }

    @Test
    void executionDiagramAndEvents() throws Exception {
        JsonNode started = executeJsonRPCMethod("startJob", Map.of("jobName", "flow", "parameters", Map.of()));
        assertTrue(started.get("success").asBoolean(), started.toString());
        long executionId = started.get("executionId").asLong();
        awaitExecution(executionId, BatchStatus.COMPLETED);

        String dot = executeJsonRPCMethod(String.class, "getExecutionDiagram", Map.of("executionId", executionId));
        assertEquals(4, dot.split("fillcolor=\"#d1f2d9\"", -1).length - 1, dot);

        List<JsonNode> events = new ArrayList<>();
        for (JsonNode event : executeJsonRPCMethod("getRecentEvents")) {
            if (event.get("executionId").asLong() == executionId) {
                events.add(event);
            }
        }
        assertEquals(10, events.size(), events.toString());
        assertEquals("JOB_STARTED", events.get(0).get("type").asString());
        assertEquals("flow-job", events.get(0).get("jobName").asString());
        JsonNode jobEnded = events.get(events.size() - 1);
        assertEquals("JOB_ENDED", jobEnded.get("type").asString());
        assertEquals("COMPLETED", jobEnded.get("batchStatus").asString());
        List<String> stepsEnded = new ArrayList<>();
        for (JsonNode event : events) {
            if (event.get("type").asString().equals("STEP_ENDED")) {
                assertEquals("COMPLETED", event.get("batchStatus").asString(), event.toString());
                stepsEnded.add(event.get("stepName").asString());
            }
        }
        assertEquals(4, stepsEnded.size(), stepsEnded.toString());
        assertTrue(stepsEnded.containsAll(List.of("prepare", "left-step", "right-step", "report")), stepsEnded.toString());
    }

    @Test
    void schedules() throws Exception {
        assertEquals(1, executeJsonRPCMethod(Integer.class, "getScheduleCount"));
        JsonNode schedules = executeJsonRPCMethod("getSchedules");
        assertTrue(schedules.get("schedulerStarted").asBoolean());
        assertEquals(1, schedules.get("schedules").size());
        JsonNode schedule = schedules.get("schedules").get(0);
        String scheduleId = schedule.get("id").asString();
        assertEquals("batchlet", schedule.get("jobName").asString());
        assertEquals("0 0 0 1 1 ?", schedule.get("cron").asString());
        assertEquals("david", schedule.get("parameters").get("name").asString());
        assertFalse(schedule.get("paused").asBoolean());
        assertFalse(schedule.get("nextFireTime").isNull());

        assertTrue(executeJsonRPCMethod("pauseSchedule", Map.of("scheduleId", scheduleId)).get("success").asBoolean());
        assertTrue(executeJsonRPCMethod("getSchedules").get("schedules").get(0).get("paused").asBoolean());
        assertTrue(executeJsonRPCMethod("resumeSchedule", Map.of("scheduleId", scheduleId)).get("success").asBoolean());
        assertFalse(executeJsonRPCMethod("getSchedules").get("schedules").get(0).get("paused").asBoolean());

        assertTrue(executeJsonRPCMethod("cancelSchedule", Map.of("scheduleId", scheduleId)).get("success").asBoolean());
        assertEquals(0, executeJsonRPCMethod("getSchedules").get("schedules").size());
        assertFalse(executeJsonRPCMethod("cancelSchedule", Map.of("scheduleId", scheduleId)).get("success").asBoolean());
    }

    @Test
    void startFailRestartAndAbandon() throws Exception {
        // Fails, because the batchlet expects the name david
        JsonNode started = executeJsonRPCMethod("startJob",
                Map.of("jobName", "batchlet", "parameters", Map.of("name", "joe")));
        assertTrue(started.get("success").asBoolean(), started.toString());
        long failedExecutionId = started.get("executionId").asLong();

        JsonNode failed = awaitExecution(failedExecutionId, BatchStatus.FAILED);
        assertEquals("batchlet-job", failed.get("jobName").asString());
        assertEquals("joe", failed.get("parameters").get("name").asString());
        assertFalse(failed.get("stoppable").asBoolean());
        assertTrue(failed.get("restartable").asBoolean());
        assertTrue(failed.get("abandonable").asBoolean());

        JsonNode steps = executeJsonRPCMethod("getStepExecutions", Map.of("executionId", failedExecutionId));
        assertEquals(1, steps.size());
        assertEquals("batchlet-step", steps.get(0).get("stepName").asString());
        assertEquals("FAILED", steps.get(0).get("batchStatus").asString());
        assertTrue(steps.get(0).get("exception").asString().contains("Unexpected value injected to 'name': joe"));

        // The failure is published as an event
        JsonNode stepFailed = null;
        for (JsonNode event : executeJsonRPCMethod("getRecentEvents")) {
            if (event.get("executionId").asLong() == failedExecutionId && event.get("type").asString().equals("STEP_ENDED")) {
                stepFailed = event;
            }
        }
        assertNotNull(stepFailed);
        assertEquals("FAILED", stepFailed.get("batchStatus").asString());
        assertTrue(stepFailed.get("message").asString().contains("Unexpected value injected to 'name': joe"));

        // The failed step is colored in the execution diagram
        String dot = executeJsonRPCMethod(String.class, "getExecutionDiagram", Map.of("executionId", failedExecutionId));
        assertTrue(dot.contains("fillcolor=\"#f9d6d5\""), dot);

        // Restart with the expected name
        JsonNode restarted = executeJsonRPCMethod("restartExecution",
                Map.of("executionId", failedExecutionId, "parameters", Map.of("name", "david")));
        assertTrue(restarted.get("success").asBoolean(), restarted.toString());
        long restartedExecutionId = restarted.get("executionId").asLong();

        JsonNode completed = awaitExecution(restartedExecutionId, BatchStatus.COMPLETED);
        assertEquals(failed.get("instanceId").asLong(), completed.get("instanceId").asLong());
        assertFalse(completed.get("restartable").asBoolean());
        assertFalse(findExecution(failedExecutionId).get("restartable").asBoolean(),
                "Only the most recent execution of an instance can be restarted");

        // Abandon the original execution
        JsonNode abandoned = executeJsonRPCMethod("abandonExecution", Map.of("executionId", failedExecutionId));
        assertTrue(abandoned.get("success").asBoolean(), abandoned.toString());
        JsonNode execution = findExecution(failedExecutionId);
        assertEquals("ABANDONED", execution.get("batchStatus").asString());
        assertFalse(execution.get("abandonable").asBoolean());
    }

    @Test
    void startWithConfiguredParameters() throws Exception {
        JsonNode started = executeJsonRPCMethod("startJob", Map.of("jobName", "batchlet", "parameters", Map.of()));
        assertTrue(started.get("success").asBoolean(), started.toString());
        JsonNode execution = awaitExecution(started.get("executionId").asLong(), BatchStatus.COMPLETED);
        assertEquals("david", execution.get("parameters").get("name").asString());

        // The executions are stored with the Job id, but filtered with the Job XML name
        JsonNode filtered = executeJsonRPCMethod("getExecutions", Map.of("jobName", "batchlet"));
        assertTrue(filtered.size() > 0);
        for (JsonNode filteredExecution : filtered) {
            assertEquals("batchlet-job", filteredExecution.get("jobName").asString());
        }
        JsonNode lastExecution = executeJsonRPCMethod("getJobs").get(0).get("lastExecution");
        assertEquals("batchlet-job", lastExecution.get("jobName").asString());
        assertTrue(lastExecution.get("executionId").asLong() >= execution.get("executionId").asLong());
        JsonNode unknown = executeJsonRPCMethod("getExecutions", Map.of("jobName", "unknown"));
        assertEquals(0, unknown.size());
    }

    @Test
    void startUnknownJob() throws Exception {
        JsonNode started = executeJsonRPCMethod("startJob", Map.of("jobName", "unknown", "parameters", Map.of()));
        assertFalse(started.get("success").asBoolean());
        assertTrue(started.get("message").asString().contains("unknown"));
    }

    @Test
    void stopExecution() throws Exception {
        JsonNode started = executeJsonRPCMethod("startJob", Map.of("jobName", "stoppable", "parameters", Map.of()));
        assertTrue(started.get("success").asBoolean(), started.toString());
        long executionId = started.get("executionId").asLong();

        JsonNode running = awaitExecution(executionId, BatchStatus.STARTED);
        assertEquals("stoppable-job", running.get("jobName").asString());
        assertTrue(running.get("stoppable").asBoolean());
        assertFalse(running.get("restartable").asBoolean());
        assertFalse(running.get("abandonable").asBoolean());

        JsonNode stopped = executeJsonRPCMethod("stopExecution", Map.of("executionId", executionId));
        assertTrue(stopped.get("success").asBoolean(), stopped.toString());
        JsonNode execution = awaitExecution(executionId, BatchStatus.STOPPED);
        assertFalse(execution.get("stoppable").asBoolean());
        assertTrue(execution.get("restartable").asBoolean());

        // Not running anymore
        assertFalse(executeJsonRPCMethod("stopExecution", Map.of("executionId", executionId)).get("success").asBoolean());
    }

    @Test
    void streamEvents() throws Exception {
        Queue<JsonNode> events = new ConcurrentLinkedQueue<>();
        withWebSocket(socket -> {
            socket.textMessageHandler(message -> {
                JsonNode result = MAPPER.readTree(message).get("result");
                if (result != null && "SubscriptionMessage".equals(result.path("messageType").asString())) {
                    events.add(result.get("object"));
                }
            });
            socket.writeTextMessage(jsonRpcRequest("streamEvents", Map.of())).await();

            // The request is handled asynchronously, so start Jobs until the subscription receives an event
            List<Long> executionIds = new ArrayList<>();
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                executionIds.add(executeJsonRPCMethod("startJob", Map.of("jobName", "batchlet", "parameters", Map.of()))
                        .get("executionId").asLong());
                TimeUnit.MILLISECONDS.sleep(500);
                return events.stream().anyMatch(event -> event.get("type").asString().equals("JOB_ENDED"));
            });

            JsonNode jobEnded = events.stream()
                    .filter(event -> event.get("type").asString().equals("JOB_ENDED"))
                    .findFirst().orElseThrow();
            assertTrue(executionIds.contains(jobEnded.get("executionId").asLong()), jobEnded.toString());
            assertEquals("batchlet-job", jobEnded.get("jobName").asString());
            assertEquals("COMPLETED", jobEnded.get("batchStatus").asString());
        });
    }

    @Test
    void assistantNotAvailable() throws Exception {
        JsonNode started = executeJsonRPCMethod("startJob", Map.of("jobName", "batchlet", "parameters", Map.of()));
        long executionId = started.get("executionId").asLong();
        awaitExecution(executionId, BatchStatus.COMPLETED);

        for (String method : List.of("explainFailure", "generateJob")) {
            Map<String, Object> params = method.equals("explainFailure")
                    ? Map.of("executionId", executionId)
                    : Map.of("description", "A job");
            CompletableFuture<JsonNode> response = new CompletableFuture<>();
            withWebSocket(socket -> {
                socket.textMessageHandler(message -> response.complete(MAPPER.readTree(message)));
                socket.writeTextMessage(jsonRpcRequest(method, params)).await();
                JsonNode error = response.get(10, TimeUnit.SECONDS).get("error");
                assertNotNull(error, response.get().toString());
                assertTrue(error.get("message").asString().contains("The Dev Assistant is not available"), error.toString());
            });
        }
    }

    /**
     * {@link DevUIJsonRPCTest} does not support subscriptions, and retries until it times out on an error response.
     */
    private void withWebSocket(final WebSocketConsumer consumer) throws Exception {
        Vertx vertx = Vertx.vertx();
        WebSocketClient client = vertx.createWebSocketClient(new WebSocketClientOptions()
                .setDefaultHost(jsonRpcUri.getHost())
                .setDefaultPort(jsonRpcUri.getPort()));
        try {
            consumer.accept(client.connect(jsonRpcUri.getPath()).await());
        } finally {
            client.close().await();
            vertx.close().await();
        }
    }

    private static String jsonRpcRequest(final String method, final Map<String, Object> params) {
        ObjectNode request = MAPPER.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "quarkus-jberet_" + method);
        request.set("params", MAPPER.valueToTree(params));
        return request.toString();
    }

    interface WebSocketConsumer {
        void accept(WebSocket socket) throws Exception;
    }

    private JsonNode awaitExecution(long executionId, BatchStatus status) throws Exception {
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            JsonNode execution = findExecution(executionId);
            return execution != null && status.name().equals(execution.get("batchStatus").asString());
        });
        JsonNode execution = findExecution(executionId);
        assertNotNull(execution);
        return execution;
    }

    private static List<String> strings(JsonNode array) {
        List<String> strings = new ArrayList<>();
        array.forEach(node -> strings.add(node.asString()));
        return strings;
    }

    private JsonNode findExecution(long executionId) throws Exception {
        for (JsonNode execution : executeJsonRPCMethod("getExecutions", Map.of("jobName", ""))) {
            if (execution.get("executionId").asLong() == executionId) {
                return execution;
            }
        }
        return null;
    }

    @Named("noop")
    @Dependent
    public static class NoopBatchlet implements Batchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }

        @Override
        public void stop() {
        }
    }

    @Named("spareBatchlet")
    @Dependent
    public static class SpareBatchlet implements Batchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }

        @Override
        public void stop() {
        }
    }

    @Named("flowDecider")
    @Dependent
    public static class FlowDecider implements Decider {
        @Override
        public String decide(StepExecution[] executions) {
            return "split";
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

    @Named("waitBatchlet")
    @Dependent
    public static class WaitBatchlet implements Batchlet {
        private volatile boolean stopped;

        @Override
        public String process() throws Exception {
            long timeout = System.currentTimeMillis() + 10_000;
            while (!stopped && System.currentTimeMillis() < timeout) {
                TimeUnit.MILLISECONDS.sleep(50);
            }
            return BatchStatus.STOPPED.toString();
        }

        @Override
        public void stop() {
            stopped = true;
        }
    }
}
