package io.quarkiverse.jberet.it.opentelemetry;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Properties;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TracingTest {
    @Inject
    InMemorySpanExporter exporter;
    @Inject
    OpenTelemetry openTelemetry;

    @BeforeEach
    void setUp() {
        exporter.reset();
    }

    @Test
    void batchlet() {
        Span parent = openTelemetry.getTracer("test").spanBuilder("parent").startSpan();
        try (Scope ignored = parent.makeCurrent()) {
            start("batchlet");
        } finally {
            parent.end();
        }

        List<SpanData> spans = awaitSpans("batchlet", 3);
        SpanData job = span(spans, "BatchJob batchlet");
        SpanData step = span(spans, "BatchJob batchlet.step");

        assertEquals(SpanKind.INTERNAL, job.getKind());
        assertEquals("io.quarkiverse.jberet", job.getInstrumentationScopeInfo().getName());
        assertEquals(parent.getSpanContext().getTraceId(), job.getTraceId());
        assertEquals(parent.getSpanContext().getSpanId(), job.getParentSpanId());
        assertEquals(StatusCode.UNSET, job.getStatus().getStatusCode());

        assertEquals(job.getSpanId(), step.getParentSpanId());
        assertEquals(StatusCode.UNSET, step.getStatus().getStatusCode());
    }

    @Test
    void batchletFailing() {
        start("batchlet-failing");

        List<SpanData> spans = awaitSpans("batchlet-failing", 2);
        SpanData job = span(spans, "BatchJob batchlet-failing");
        SpanData step = span(spans, "BatchJob batchlet-failing.step");

        assertFalse(job.getParentSpanContext().isValid());
        assertEquals(StatusCode.ERROR, job.getStatus().getStatusCode());
        assertEquals(job.getSpanId(), step.getParentSpanId());
        assertEquals(StatusCode.ERROR, step.getStatus().getStatusCode());
        assertTrue(step.getEvents().stream().anyMatch(event -> event.getName().equals("exception")));
    }

    @Test
    void chunk() {
        start("chunk");

        // 1 Job, 1 Step, 2 Chunks
        List<SpanData> spans = awaitSpans("chunk", 4);
        SpanData job = span(spans, "BatchJob chunk");
        SpanData step = span(spans, "BatchJob chunk.step");
        List<SpanData> chunks = spans(spans, "BatchJob chunk.step.Chunk");

        assertEquals(job.getSpanId(), step.getParentSpanId());
        assertEquals(2, chunks.size());
        chunks.forEach(chunk -> assertEquals(step.getSpanId(), chunk.getParentSpanId()));
    }

    @Test
    void partition() {
        start("partition");

        // 1 Job, 1 Step, 2 Partitions with 1 Chunk each
        List<SpanData> spans = awaitSpans("partition", 4);
        SpanData job = span(spans, "BatchJob partition");
        SpanData step = span(spans, "BatchJob partition.step");
        List<SpanData> chunks = spans(spans, "BatchJob partition.step.Chunk");

        assertEquals(job.getSpanId(), step.getParentSpanId());
        assertEquals(2, chunks.size());
        // The Partitions execute in other threads
        chunks.forEach(chunk -> assertEquals(step.getSpanId(), chunk.getParentSpanId()));
        chunks.forEach(chunk -> assertEquals(job.getTraceId(), chunk.getTraceId()));
    }

    @Test
    void split() {
        start("split");

        List<SpanData> spans = awaitSpans("split", 3);
        SpanData job = span(spans, "BatchJob split");
        SpanData step1 = span(spans, "BatchJob split.step1");
        SpanData step2 = span(spans, "BatchJob split.step2");

        // The Flows execute in other threads
        assertEquals(job.getSpanId(), step1.getParentSpanId());
        assertEquals(job.getSpanId(), step2.getParentSpanId());
    }

    private static void start(final String jobName) {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start(jobName, new Properties());
        await().atMost(5, SECONDS).until(() -> {
            BatchStatus status = jobOperator.getJobExecution(executionId).getBatchStatus();
            return status == BatchStatus.COMPLETED || status == BatchStatus.FAILED;
        });
    }

    private List<SpanData> awaitSpans(final String jobName, final int count) {
        await().atMost(5, SECONDS).until(() -> jobSpans(jobName).size() >= count);
        List<SpanData> spans = jobSpans(jobName);
        assertEquals(count, spans.size(), () -> spans.stream().map(SpanData::getName).toList().toString());
        return spans;
    }

    private List<SpanData> jobSpans(final String jobName) {
        return exporter.getFinishedSpanItems().stream()
                .filter(span -> span.getName().equals("BatchJob " + jobName)
                        || span.getName().startsWith("BatchJob " + jobName + ".")
                        || span.getName().equals("parent"))
                .collect(toList());
    }

    private static SpanData span(final List<SpanData> spans, final String name) {
        List<SpanData> found = spans(spans, name);
        assertEquals(1, found.size(), name);
        return found.get(0);
    }

    private static List<SpanData> spans(final List<SpanData> spans, final String name) {
        return spans.stream().filter(span -> span.getName().equals(name)).toList();
    }
}
